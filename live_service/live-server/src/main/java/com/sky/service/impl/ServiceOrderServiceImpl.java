package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.ServiceOrderDispatchDTO;
import com.sky.dto.ServiceOrderPageQueryDTO;
import com.sky.dto.ServiceOrderRejectionDTO;
import com.sky.dto.ServiceOrderSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Provider;
import com.sky.entity.ServiceArea;
import com.sky.entity.ServiceCart;
import com.sky.entity.ServiceItem;
import com.sky.entity.ServiceOrder;
import com.sky.entity.ServiceOrderItem;
import com.sky.entity.Slot;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ProviderNotAvailableException;
import com.sky.exception.ServiceAreaNotOpenException;
import com.sky.exception.SlotNotAvailableException;
import com.sky.exception.SlotSoldOutException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.ProviderMapper;
import com.sky.mapper.ServiceAreaMapper;
import com.sky.mapper.ServiceCartMapper;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServiceOrderItemMapper;
import com.sky.mapper.ServiceOrderMapper;
import com.sky.mapper.SlotMapper;
import com.sky.mq.producer.OrderMessageProducer;
import com.sky.result.PageResult;
import com.sky.service.ServiceOrderService;
import com.sky.service.ShopService;
import com.sky.service.SlotService;
import com.sky.vo.ServiceOrderStatisticsVO;
import com.sky.vo.ServiceOrderSubmitVO;
import com.sky.vo.ServiceOrderVO;
import com.sky.websocket.AdminNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务订单业务实现
 * <p>
 * 这个类把三样东西串在了一起：
 * Redis 原子预扣（防超卖）+ MySQL CAS 落账（幂等）+ MQ 延迟队列（超时取消）。
 */
@Service
@Slf4j
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 推送给管理端的提醒里展示预约时间的格式 */
    private static final DateTimeFormatter NOTIFY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /**
     * 自动转派次数上限
     * <p>
     * 不做限制的话，只要一直没人接单，订单就会每 5 分钟换一个师傅、
     * 无限循环下去：日志被刷爆、师傅被反复打扰，
     * 而用户永远等不到一个确定的结果。
     * 超过这个次数就停下来等人工派单。
     */
    private static final int MAX_DISPATCH_COUNT = 3;

    @Autowired
    private ServiceOrderMapper serviceOrderMapper;
    @Autowired
    private ServiceOrderItemMapper serviceOrderItemMapper;
    @Autowired
    private ServiceCartMapper serviceCartMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private SlotMapper slotMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ServiceAreaMapper serviceAreaMapper;
    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private SlotService slotService;
    @Autowired
    private OrderMessageProducer orderMessageProducer;
    @Autowired
    private AdminNotifier adminNotifier;
    @Autowired
    private ShopService shopService;

    // ========================================================================
    //  下单
    // ========================================================================

    @Override
    @Transactional
    public ServiceOrderSubmitVO submitOrder(ServiceOrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();

        // 1. 平台打烊期间不允许下单
        //    管理端页面上写着「打烊后用户端无法提交订单」，
        //    但原实现只在管理端存了个 Redis 状态、下单流程里根本没读，
        //    等于这个开关是摆设——打烊了照样能下单
        if (!ShopService.STATUS_OPEN.equals(shopService.getStatus())) {
            throw new OrderBusinessException(MessageConstant.SHOP_CLOSED);
        }

        // 2. 校验时段
        Slot slot = slotMapper.getById(dto.getSlotId());
        if (slot == null || !Slot.STATUS_OPEN.equals(slot.getStatus())) {
            throw new SlotNotAvailableException(MessageConstant.SLOT_NOT_FOUND);
        }

        // 3. 取服务清单
        List<ServiceCart> cartItems = serviceCartMapper.list(
                ServiceCart.builder().userId(userId).build());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 4. 上门服务要校验地址和服务区域
        AddressBook address = null;
        if (ServiceOrder.MODE_HOME.equals(dto.getServiceMode())) {
            if (dto.getAddressBookId() == null) {
                throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
            }
            address = addressBookMapper.getById(dto.getAddressBookId());
            checkServiceArea(address);
        }

        // 5. 【核心】Redis Lua 原子预扣名额
        //    放在落库之前：抢不到名额就直接返回，不产生任何数据库写入
        slotService.preDeduct(slot.getId(), userId);

        try {
            BigDecimal amount = BigDecimal.ZERO;
            List<ServiceOrderItem> orderItems = new ArrayList<>();
            for (ServiceCart cart : cartItems) {
                BigDecimal itemAmount = cart.getAmount()
                        .multiply(BigDecimal.valueOf(cart.getNumber()));
                amount = amount.add(itemAmount);
                orderItems.add(ServiceOrderItem.builder()
                        .serviceId(cart.getServiceId())
                        .servicePackageId(cart.getServicePackageId())
                        .name(cart.getName())
                        .spec(cart.getSpec())
                        .price(cart.getAmount())
                        .number(cart.getNumber())
                        .amount(itemAmount)
                        .image(cart.getImage())
                        .duration(resolveDuration(cart))
                        .build());
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime serviceTime = LocalDateTime.of(slot.getServiceDate(), slot.getStartTime());

            ServiceOrder order = ServiceOrder.builder()
                    .number(generateOrderNumber())
                    .status(ServiceOrder.PENDING_PAYMENT)
                    .userId(userId)
                    .addressBookId(dto.getAddressBookId())
                    .slotId(slot.getId())
                    .providerId(slot.getProviderId())
                    .serviceMode(dto.getServiceMode())
                    .serviceTime(serviceTime)
                    .serviceDuration(slot.getEndTime().toSecondOfDay() - slot.getStartTime().toSecondOfDay())
                    .amount(amount)
                    .payMethod(dto.getPayMethod())
                    .payStatus(ServiceOrder.UN_PAID)
                    .orderTime(now)
                    .userName(address == null ? null : address.getConsignee())
                    .consignee(address == null ? null : address.getConsignee())
                    .phone(address == null ? null : address.getPhone())
                    .address(address == null ? null : buildFullAddress(address))
                    .remark(dto.getRemark())
                    .verifyCode(generateVerifyCode())
                    .dispatchCount(0)
                    .merchantId(1L)
                    .build();
            serviceOrderMapper.insert(order);

            for (ServiceOrderItem item : orderItems) {
                item.setOrderId(order.getId());
            }
            serviceOrderItemMapper.insertBatch(orderItems);

            // 下单成功后清单清空
            serviceCartMapper.deleteByUserId(userId);

            // 6. 发 15 分钟延迟消息：到期仍待付款就自动取消
            orderMessageProducer.sendOrderTimeout(
                    order.getId(), order.getNumber(), slot.getId(), userId);

            log.info("下单成功：orderId={}, number={}, amount={}, 预约时间={}",
                    order.getId(), order.getNumber(), amount, serviceTime);

            return ServiceOrderSubmitVO.builder()
                    .id(order.getId())
                    .orderNumber(order.getNumber())
                    .orderAmount(amount)
                    .orderTime(now)
                    .serviceTime(serviceTime)
                    .build();

        } catch (RuntimeException e) {
            // 落库失败 / 发消息异常时把 Redis 名额还回去。
            // Redis 不参与数据库事务，不显式回滚就会白白占掉一个名额，
            // 直到定时对账任务才能发现——而那时候可能已经少卖了一单。
            slotService.rollback(slot.getId(), userId);
            throw e;
        }
    }

    // ========================================================================
    //  支付与取消
    // ========================================================================

    @Override
    @Transactional
    public void paySuccess(String orderNumber, Integer payMethod) {
        ServiceOrder order = serviceOrderMapper.getByNumber(orderNumber);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 幂等：微信回调会重试，重复到达时直接跳过
        if (!ServiceOrder.PENDING_PAYMENT.equals(order.getStatus())) {
            log.info("订单不是待付款状态，跳过支付处理：number={}, status={}",
                    orderNumber, order.getStatus());
            return;
        }

        // 先落 MySQL 库存。这一步用了 CAS（where booked_count < total_stock），
        // 万一 Redis 那边超卖了，这里会拦住。
        if (!slotService.confirmBooked(order.getSlotId())) {
            log.error("MySQL 库存落账失败，名额已被抢完，订单需退款：number={}", orderNumber);
            throw new SlotSoldOutException(MessageConstant.SLOT_SOLD_OUT);
        }

        ServiceOrder update = ServiceOrder.builder()
                .id(order.getId())
                .status(ServiceOrder.TO_BE_ACCEPTED)
                .payStatus(ServiceOrder.PAID)
                .payMethod(payMethod)
                .checkoutTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.PENDING_PAYMENT);
        if (rows == 0) {
            // 并发下已被别的线程处理过，把刚才多扣的 MySQL 库存还回去
            slotService.releaseBooked(order.getSlotId());
            log.info("订单状态已被并发修改，本次支付处理作废：number={}", orderNumber);
            return;
        }

        // 支付成功后排一条「服务前 1 小时提醒」
        orderMessageProducer.sendServiceRemind(
                order.getId(), order.getUserId(), order.getProviderId(), order.getServiceTime());

        // 订单进入待接单，师傅需要在 5 分钟内接单，超时自动转派给其他人
        orderMessageProducer.sendDispatchTimeout(order.getId(), order.getProviderId());

        // 实时推送给管理端：新订单待接单，页面弹窗 + 提示音。
        // 走 WebSocket 而不是让运营刷新列表，派单响应速度差很多
        adminNotifier.newOrder(order.getId(), String.format(
                "订单 %s 已支付，预约时间 %s，服务地址：%s，请及时派单",
                order.getNumber(),
                order.getServiceTime() == null ? "-" : NOTIFY_TIME_FORMAT.format(order.getServiceTime()),
                order.getAddress() == null ? "到店服务" : order.getAddress()));

        log.info("支付成功：number={}, 订单进入待接单", orderNumber);
    }

    @Override
    @Transactional
    public boolean timeoutCancel(Long orderId) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            log.info("订单不存在，跳过超时取消：orderId={}", orderId);
            return false;
        }
        if (!ServiceOrder.PENDING_PAYMENT.equals(order.getStatus())) {
            log.info("订单已被处理（当前状态{}），跳过超时取消：orderId={}",
                    order.getStatus(), orderId);
            return false;
        }

        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.CANCELLED)
                .cancelReason("订单超时，自动取消")
                .cancelTime(LocalDateTime.now())
                .build();
        // CAS：只有订单仍然是待付款才更新成功。
        // 这一步同时挡住了「用户刚好在这一刻付款」的竞态——
        // 如果用户已付款，status 已变成 2，这里影响行数为 0，直接跳过。
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.PENDING_PAYMENT);
        if (rows == 0) {
            log.info("订单状态已被并发修改，本次超时取消作废：orderId={}", orderId);
            return false;
        }

        // 关单成功后把 Redis 名额还回去（脚本内部做了幂等）
        slotService.rollback(order.getSlotId(), order.getUserId());
        log.info("订单超时已自动取消并回补名额：orderId={}, slotId={}",
                orderId, order.getSlotId());
        return true;
    }

    @Override
    @Transactional
    public void userCancel(Long orderId) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 只有还没派单出去的订单允许用户自己取消
        Integer status = order.getStatus();
        if (!ServiceOrder.PENDING_PAYMENT.equals(status) && !ServiceOrder.TO_BE_ACCEPTED.equals(status)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, status);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        slotService.rollback(order.getSlotId(), order.getUserId());
        // 已付款的要释放 MySQL 里的占用
        if (ServiceOrder.PAID.equals(order.getPayStatus())) {
            slotService.releaseBooked(order.getSlotId());
        }
        log.info("用户取消订单：orderId={}", orderId);
    }

    // ========================================================================
    //  查询
    // ========================================================================

    @Override
    public PageResult pageQuery(ServiceOrderPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<ServiceOrder> page = serviceOrderMapper.pageQuery(dto);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public ServiceOrderVO getDetail(Long orderId) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<ServiceOrderItem> items = serviceOrderItemMapper.listByOrderId(orderId);

        ServiceOrderVO vo = new ServiceOrderVO();
        vo.setOrder(order);
        vo.setItems(items);
        vo.setServiceNames(buildServiceNames(items));
        if (order.getProviderId() != null) {
            Provider provider = providerMapper.getById(order.getProviderId());
            if (provider != null) {
                vo.setProviderName(provider.getName());
                vo.setProviderPhone(provider.getPhone());
                vo.setProviderAvatar(provider.getAvatar());
            }
        }
        return vo;
    }

    @Override
    @Transactional
    public boolean reassign(Long orderId, Long excludeProviderId) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            log.info("订单不存在，跳过转派：orderId={}", orderId);
            return false;
        }
        // 只在「待接单」状态下转派。已经接单、已取消、已完成的都不用管
        if (!ServiceOrder.TO_BE_ACCEPTED.equals(order.getStatus())) {
            log.info("订单已不是待接单状态（当前 {}），跳过转派：orderId={}",
                    order.getStatus(), orderId);
            return false;
        }

        // 转派次数上限检查。没有这道限制的话，
        // 只要一直没人接单，订单每 5 分钟就会换一个人、永远循环下去
        int nextCount = (order.getDispatchCount() == null ? 0 : order.getDispatchCount()) + 1;
        if (nextCount > MAX_DISPATCH_COUNT) {
            log.warn("订单已自动转派 {} 次仍无人接单，停止自动转派，等待人工派单：orderId={}",
                    MAX_DISPATCH_COUNT, orderId);
            return false;
        }

        Long newProviderId;
        try {
            newProviderId = autoSelectProvider(order, excludeProviderId);
        } catch (Exception e) {
            // 没有可接单的师傅不算异常：订单留在待接单里等人工处理
            log.warn("没有找到可转派的师傅，订单继续等待人工派单：orderId={}", orderId);
            return false;
        }

        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.TO_BE_ACCEPTED)
                .providerId(newProviderId)
                .dispatchCount(nextCount)
                .dispatchTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.TO_BE_ACCEPTED);
        if (rows == 0) {
            return false;
        }

        // 新师傅同样只有 5 分钟，超时继续往下转
        orderMessageProducer.sendDispatchTimeout(orderId, newProviderId);
        log.info("派单超时已转派：orderId={}, 原师傅={}, 新师傅={}",
                orderId, excludeProviderId, newProviderId);
        return true;
    }

    @Override
    public ServiceOrderStatisticsVO statistics() {
        return ServiceOrderStatisticsVO.builder()
                .toBeAccepted(serviceOrderMapper.countByStatus(ServiceOrder.TO_BE_ACCEPTED))
                .accepted(serviceOrderMapper.countByStatus(ServiceOrder.ACCEPTED))
                .inService(serviceOrderMapper.countByStatus(ServiceOrder.IN_SERVICE))
                .toBeReviewed(serviceOrderMapper.countByStatus(ServiceOrder.TO_BE_REVIEWED))
                .build();
    }

    // ========================================================================
    //  派单与服务过程
    // ========================================================================

    @Override
    @Transactional
    public void dispatch(ServiceOrderDispatchDTO dto) {
        ServiceOrder order = serviceOrderMapper.getById(dto.getOrderId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ServiceOrder.TO_BE_ACCEPTED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Long providerId = resolveProvider(order, dto.getProviderId());

        ServiceOrder update = ServiceOrder.builder()
                .id(order.getId())
                .status(ServiceOrder.TO_BE_ACCEPTED)
                .providerId(providerId)
                .dispatchTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.TO_BE_ACCEPTED);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 5 分钟没接单就自动转派
        orderMessageProducer.sendDispatchTimeout(order.getId(), providerId);
        log.info("派单成功：orderId={}, providerId={}", order.getId(), providerId);
    }

    @Override
    @Transactional
    public void accept(Long orderId, Long providerId) {
        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.ACCEPTED)
                .providerId(providerId)
                .acceptTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.TO_BE_ACCEPTED);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        log.info("师傅接单：orderId={}, providerId={}", orderId, providerId);
    }

    @Override
    @Transactional
    public void reject(ServiceOrderRejectionDTO dto) {
        ServiceOrder update = ServiceOrder.builder()
                .id(dto.getId())
                .status(ServiceOrder.TO_BE_ACCEPTED)
                .rejectionReason(dto.getRejectionReason())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.ACCEPTED);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 退回待派单状态，由管理端重新派单（也可以改成自动转派）
        log.info("师傅拒单，订单退回待派单：orderId={}, 原因={}", dto.getId(), dto.getRejectionReason());
    }

    @Override
    @Transactional
    public void startService(Long orderId, Long providerId) {
        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.IN_SERVICE)
                .startTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.ACCEPTED);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        log.info("服务开始：orderId={}, providerId={}", orderId, providerId);
    }

    @Override
    @Transactional
    public void completeService(Long orderId, Long providerId) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.TO_BE_REVIEWED)
                .finishTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.IN_SERVICE);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 回写师傅接单数和各服务项目的销量（冗余字段，列表排序要用）
        if (order.getProviderId() != null) {
            providerMapper.increaseOrderCount(order.getProviderId());
        }
        List<ServiceOrderItem> items = serviceOrderItemMapper.listByOrderId(orderId);
        for (ServiceOrderItem item : items) {
            if (item.getServiceId() != null) {
                serviceItemMapper.increaseSales(item.getServiceId(), item.getNumber());
            }
        }

        // 7 天不评价就自动结单，避免订单永远挂在待评价
        orderMessageProducer.sendAutoReview(orderId, order.getUserId(), order.getProviderId());
        log.info("服务完成，进入待评价：orderId={}", orderId);
    }

    @Override
    @Transactional
    public boolean autoComplete(Long orderId) {
        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.COMPLETED)
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.TO_BE_REVIEWED);
        if (rows == 0) {
            log.info("订单不是待评价状态，跳过自动结单：orderId={}", orderId);
            return false;
        }
        log.info("用户长期未评价，订单自动结单：orderId={}", orderId);
        return true;
    }

    @Override
    @Transactional
    public void verify(Long orderId, String verifyCode) {
        ServiceOrder order = serviceOrderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (verifyCode == null || !verifyCode.equals(order.getVerifyCode())) {
            throw new OrderBusinessException("核销码不正确");
        }
        ServiceOrder update = ServiceOrder.builder()
                .id(orderId)
                .status(ServiceOrder.IN_SERVICE)
                .verifyTime(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .build();
        int rows = serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.ACCEPTED);
        if (rows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        log.info("到店核销成功，服务开始：orderId={}", orderId);
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    /**
     * 决定这一单派给谁
     * <p>
     * 优先级：
     * <ol>
     *   <li>管理端明确指定了师傅 —— 用指定的，这是「改派」场景</li>
     *   <li>订单本身已经带了师傅 —— 保持不动</li>
     *   <li>以上都没有 —— 走自动派单算法</li>
     * </ol>
     * <p>
     * <b>第 2 条是关键。</b>本项目是「按师傅排班」的模型：
     * 用户在小程序里选时段时，选中的 slot 本身就有 provider_id，
     * 也就是说下单那一刻师傅就已经定了。
     * <p>
     * 如果这里再跑一遍算法按评分重新挑人，会出两个问题：
     * 被占用的时段属于 A，订单却派给了 B —— A 的档期被占着却不用干活；
     * 而 B 可能在自己的同时段还有别的单，档期冲突。
     */
    private Long resolveProvider(ServiceOrder order, Long assignedProviderId) {
        if (assignedProviderId != null) {
            return assignedProviderId;
        }
        if (order.getProviderId() != null) {
            return order.getProviderId();
        }
        // 没有排期归属的订单（到店服务、临时加单）才需要算法挑人
        return autoSelectProvider(order, null);
    }

    /**
     * 自动派单算法
     * <pre>
     *   1. 技能过滤：只挑掌握了该服务所属分类的师傅
     *   2. 状态过滤：只挑当前可接单的
     *   3. 排序    ：评分高的优先、接单数少的优先（负担均衡）
     *   4. 档期过滤：排除该时间段已有订单在身的师傅（按时间重叠判断）
     *   5. 取第一个
     * </pre>
     * 第 1~3 步合并成一条 SQL（ProviderMapper.listAvailableByCategoryId），
     * 第 4 步必须放在 Java 里做，因为它要逐人查订单表，属于行级判断。
     *
     * @param excludeProviderId 需要排除的师傅（转派时排除原师傅），可为 null
     */
    private Long autoSelectProvider(ServiceOrder order, Long excludeProviderId) {
        List<ServiceOrderItem> items = serviceOrderItemMapper.listByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new OrderBusinessException("订单没有服务明细，无法派单");
        }
        Long serviceId = items.get(0).getServiceId();
        if (serviceId == null) {
            throw new OrderBusinessException("套餐订单暂不支持自动派单，请手动指定");
        }
        ServiceItem item = serviceItemMapper.getById(serviceId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }

        List<Provider> candidates = providerMapper.listAvailableByCategoryId(item.getCategoryId());

        // 取订单所在时段的时间范围，用来做档期冲突判断。
        // 到店服务等没有绑定时段的订单跳过这一层过滤。
        Slot slot = order.getSlotId() == null ? null : slotMapper.getById(order.getSlotId());

        for (Provider candidate : candidates) {
            if (candidate.getId().equals(excludeProviderId)) {
                continue;
            }
            if (slot != null && isProviderBusy(candidate.getId(), slot)) {
                continue;
            }
            return candidate.getId();
        }
        throw new ProviderNotAvailableException("当前时段没有可接单的服务人员");
    }

    /**
     * 判断某师傅在目标时段是否已经有在身的订单
     * <p>
     * 判断依据是「时间区间是否重叠」，不是「slot 是否相同」：
     * 一个 slot 只属于一个师傅，按 slot 判重等于没判，
     * 订单会被派给那个时间段其实抽不开身的师傅。
     *
     * @see com.sky.mapper.ServiceOrderMapper#countProviderBusy
     */
    private boolean isProviderBusy(Long providerId, Slot slot) {
        Integer busy = serviceOrderMapper.countProviderBusy(providerId,
                slot.getServiceDate(), slot.getStartTime(), slot.getEndTime());
        return busy != null && busy > 0;
    }

    /**
     * 校验服务地址是否在平台开通的服务区域内
     */
    private void checkServiceArea(AddressBook address) {
        if (address == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        if (address.getDistrictCode() == null || address.getDistrictCode().isEmpty()) {
            throw new ServiceAreaNotOpenException(MessageConstant.SERVICE_AREA_NOT_OPEN);
        }
        ServiceArea area = serviceAreaMapper.getByCode(address.getDistrictCode());
        if (area == null || !ServiceArea.STATUS_OPEN.equals(area.getStatus())) {
            throw new ServiceAreaNotOpenException(MessageConstant.SERVICE_AREA_NOT_OPEN);
        }
    }

    /**
     * 生成订单号：17 位时间戳 + 3 位随机数
     * <p>
     * 订单号上有唯一索引，即使极端情况下撞号也会被数据库拦住，
     * 不会产生两笔同号订单。
     */
    private String generateOrderNumber() {
        return ORDER_NO_FORMAT.format(LocalDateTime.now())
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /**
     * 生成 6 位到店核销码
     */
    private String generateVerifyCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    /**
     * 拼接完整服务地址
     */
    private String buildFullAddress(AddressBook address) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, address.getProvinceName());
        // 直辖市（北京/上海/天津/重庆）的省名和市名是同一个词，
        // 直接拼会出现「北京市北京市海淀区…」，所以相同时跳过市名
        String province = address.getProvinceName() == null ? "" : address.getProvinceName();
        String city = address.getCityName() == null ? "" : address.getCityName();
        if (!city.equals(province)) {
            appendIfPresent(sb, city);
        }
        appendIfPresent(sb, address.getDistrictName());
        appendIfPresent(sb, address.getDetail());
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String part) {
        if (part != null && !part.isEmpty()) {
            sb.append(part);
        }
    }

    /**
     * 取清单项的预计服务时长
     * <p>
     * 服务项目直接用自己的 duration；套餐的话这里简化处理，
     * 用套餐自身的总时长（后续可以改成把明细时长累加）。
     */
    private Integer resolveDuration(ServiceCart cart) {
        if (cart.getServiceId() != null) {
            ServiceItem item = serviceItemMapper.getById(cart.getServiceId());
            if (item != null && item.getDuration() != null) {
                return item.getDuration();
            }
        }
        return 60;
    }

    /**
     * 拼接服务名称串，管理端列表页展示用
     */
    private String buildServiceNames(List<ServiceOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ServiceOrderItem item : items) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(item.getName());
            if (item.getNumber() != null && item.getNumber() > 1) {
                sb.append(" x").append(item.getNumber());
            }
        }
        return sb.toString();
    }
}

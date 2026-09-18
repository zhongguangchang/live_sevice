package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ServiceOrderPageQueryDTO;
import com.sky.dto.ServiceOrderPaymentDTO;
import com.sky.dto.ServiceOrderSubmitDTO;
import com.sky.exception.OrderBusinessException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ServiceOrderService;
import com.sky.vo.ServiceOrderSubmitVO;
import com.sky.vo.ServiceOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务订单（用户端）
 */
@RestController("userServiceOrderController")
@RequestMapping("/user/serviceOrder")
@Api(tags = "用户端订单接口")
@Slf4j
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService serviceOrderService;

    /**
     * 提交订单。
     * <p>
     * 服务端会依次做：校验时段与地址 -> Redis 原子预扣名额 ->
     * 落库订单 -> 发 15 分钟延迟消息。抢不到名额会直接返回失败，
     * 不会产生任何脏数据。
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<ServiceOrderSubmitVO> submit(@RequestBody ServiceOrderSubmitDTO serviceOrderSubmitDTO) {
        log.info("用户下单：{}", serviceOrderSubmitDTO);
        ServiceOrderSubmitVO vo = serviceOrderService.submitOrder(serviceOrderSubmitDTO);
        return Result.success(vo);
    }

    /**
     * 支付订单。
     * <p>
     * 真实环境这里应该调微信支付下单接口、返回预支付参数，
     * 等微信回调再改状态。毕设阶段简化为直接标记支付成功，
     * 走的是和支付回调完全相同的处理逻辑（含库存落账和幂等保护），
     * 所以后期接入真实支付时只需要替换这一个入口。
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result payment(@RequestBody ServiceOrderPaymentDTO serviceOrderPaymentDTO) {
        log.info("订单支付：{}", serviceOrderPaymentDTO);
        serviceOrderService.paySuccess(
                serviceOrderPaymentDTO.getOrderNumber(), serviceOrderPaymentDTO.getPayMethod());
        return Result.success();
    }

    /**
     * 历史订单。userId 从登录态取，不接受前端传入，
     * 否则用户可以伪造 userId 查别人的订单。
     */
    @GetMapping("/history")
    @ApiOperation("历史订单查询")
    public Result<PageResult> history(ServiceOrderPageQueryDTO serviceOrderPageQueryDTO) {
        serviceOrderPageQueryDTO.setUserId(BaseContext.getCurrentId());
        return Result.success(serviceOrderService.pageQuery(serviceOrderPageQueryDTO));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("查询订单详情")
    public Result<ServiceOrderVO> detail(@PathVariable Long id) {
        ServiceOrderVO vo = serviceOrderService.getDetail(id);
        // 越权校验：只能看自己的订单
        if (vo.getOrder() == null
                || !BaseContext.getCurrentId().equals(vo.getOrder().getUserId())) {
            throw new OrderBusinessException("无权查看该订单");
        }
        return Result.success(vo);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancel(@PathVariable Long id) {
        log.info("用户取消订单：id={}", id);
        serviceOrderService.userCancel(id);
        return Result.success();
    }
}

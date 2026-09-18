package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.ReviewPageQueryDTO;
import com.sky.dto.ReviewReplyDTO;
import com.sky.dto.ReviewSubmitDTO;
import com.sky.entity.Review;
import com.sky.entity.ServiceOrder;
import com.sky.entity.ServiceOrderItem;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.ProviderMapper;
import com.sky.mapper.ReviewMapper;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServiceOrderItemMapper;
import com.sky.mapper.ServiceOrderMapper;
import com.sky.result.PageResult;
import com.sky.service.ReviewService;
import com.sky.vo.ReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务评价业务实现
 */
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ServiceOrderMapper serviceOrderMapper;
    @Autowired
    private ServiceOrderItemMapper serviceOrderItemMapper;
    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Override
    @Transactional
    public void submit(ReviewSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();

        ServiceOrder order = serviceOrderMapper.getById(dto.getOrderId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 只有服务确实做完了才能评价
        if (!ServiceOrder.TO_BE_REVIEWED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_COMPLETED);
        }
        // 一单一评。虽然 order_id 上有唯一索引能兜住底，
        // 但先查一次能给出更友好的提示语，
        // 而不是让用户看到数据库的唯一键冲突报错
        if (reviewMapper.getByOrderId(dto.getOrderId()) != null) {
            throw new OrderBusinessException(MessageConstant.REVIEW_ALREADY_EXISTS);
        }

        // 取订单里的第一个服务项目作为被评价对象
        List<ServiceOrderItem> items = serviceOrderItemMapper.listByOrderId(dto.getOrderId());
        Long serviceId = items.isEmpty() ? null : items.get(0).getServiceId();

        Review review = Review.builder()
                .orderId(dto.getOrderId())
                .userId(userId)
                .providerId(order.getProviderId())
                .serviceId(serviceId)
                .score(dto.getScore())
                .serviceScore(dto.getServiceScore())
                .speedScore(dto.getSpeedScore())
                .qualityScore(dto.getQualityScore())
                .content(dto.getContent())
                .images(joinImages(dto.getImages()))
                .isAnonymous(dto.getIsAnonymous() == null ? 0 : dto.getIsAnonymous())
                .status(Review.STATUS_VISIBLE)
                .createTime(LocalDateTime.now())
                .build();
        reviewMapper.insert(review);

        // 把订单推进到已完成
        ServiceOrder update = ServiceOrder.builder()
                .id(order.getId())
                .status(ServiceOrder.COMPLETED)
                .build();
        serviceOrderMapper.updateStatusIfMatch(update, ServiceOrder.TO_BE_REVIEWED);

        // 回写评分。放在插入评价之后，这样聚合值已经把本次评价算进去了
        refreshProviderScore(order.getProviderId());
        refreshServiceScore(serviceId);

        log.info("评价提交成功：orderId={}, score={}, 已回写师傅与服务的评分",
                dto.getOrderId(), dto.getScore());
    }

    @Override
    public PageResult pageQuery(ReviewPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<ReviewVO> page = reviewMapper.pageQuery(dto);

        // 实体里图片是逗号分隔的字符串，前端更希望直接拿到数组
        List<ReviewVO> records = page.getResult();
        for (ReviewVO vo : records) {
            vo.setImageList(splitImages(vo));
        }
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public void reply(ReviewReplyDTO dto) {
        reviewMapper.updateReply(dto.getId(), dto.getReply());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        reviewMapper.updateStatus(id, status);
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    /**
     * 重算师傅的评分和好评率
     */
    private void refreshProviderScore(Long providerId) {
        if (providerId == null) {
            return;
        }
        BigDecimal avgScore = reviewMapper.avgScoreByProvider(providerId);
        BigDecimal goodRate = reviewMapper.goodRateByProvider(providerId);
        if (avgScore != null) {
            providerMapper.updateScore(providerId, avgScore,
                    goodRate == null ? BigDecimal.ZERO : goodRate);
            log.info("师傅评分已更新：providerId={}, score={}, goodRate={}",
                    providerId, avgScore, goodRate);
        }
    }

    /**
     * 重算服务项目的综合评分
     */
    private void refreshServiceScore(Long serviceId) {
        if (serviceId == null) {
            return;
        }
        BigDecimal avgScore = reviewMapper.avgScoreByService(serviceId);
        if (avgScore != null) {
            serviceItemMapper.updateScore(serviceId, avgScore);
            log.info("服务项目评分已更新：serviceId={}, score={}", serviceId, avgScore);
        }
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return String.join(",", images);
    }

    /**
     * 把逗号分隔的图片串拆成数组
     */
    private List<String> splitImages(ReviewVO vo) {
        String images = vo.getImages();
        if (images == null || images.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (String s : images.split(",")) {
            if (!s.trim().isEmpty()) {
                list.add(s.trim());
            }
        }
        return list;
    }
}

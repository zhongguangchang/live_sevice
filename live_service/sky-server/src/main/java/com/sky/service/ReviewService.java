package com.sky.service;

import com.sky.dto.ReviewPageQueryDTO;
import com.sky.dto.ReviewReplyDTO;
import com.sky.dto.ReviewSubmitDTO;
import com.sky.result.PageResult;

/**
 * 服务评价业务接口
 */
public interface ReviewService {

    /**
     * 提交评价
     * <p>
     * 一单只能评一次。提交后要回写三处：
     * 师傅的综合评分与好评率、服务项目的综合评分、
     * 以及把订单状态从「待评价」推进到「已完成」。
     */
    void submit(ReviewSubmitDTO reviewSubmitDTO);

    /**
     * 分页查询评价
     */
    PageResult pageQuery(ReviewPageQueryDTO reviewPageQueryDTO);

    /**
     * 平台 / 商家回复评价
     */
    void reply(ReviewReplyDTO reviewReplyDTO);

    /**
     * 显示 / 隐藏评价（运营处理恶意评价用）
     */
    void updateStatus(Long id, Integer status);
}

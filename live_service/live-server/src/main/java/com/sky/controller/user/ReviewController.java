package com.sky.controller.user;

import com.sky.annotation.RateLimit;
import com.sky.dto.ReviewPageQueryDTO;
import com.sky.dto.ReviewSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务评价（用户端）
 */
@RestController("userReviewController")
@RequestMapping("/user/review")
@Api(tags = "用户端评价接口")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 提交评价。三维打分：服务态度、响应速度、服务质量。
     * 提交后会同步回写师傅评分、服务项目评分，并把订单推进到已完成。
     */
    @PostMapping("/submit")
    @ApiOperation("提交评价")
    @RateLimit(key = "review:submit", seconds = 10, limit = 3)
    public Result submit(@RequestBody ReviewSubmitDTO reviewSubmitDTO) {
        log.info("提交评价：{}", reviewSubmitDTO);
        reviewService.submit(reviewSubmitDTO);
        return Result.success();
    }

    /**
     * 查某个服务的评价列表，服务详情页用
     */
    @GetMapping("/list")
    @ApiOperation("查询服务的评价列表")
    public Result<PageResult> list(ReviewPageQueryDTO reviewPageQueryDTO) {
        // 用户端只能看到状态为「显示」的评价
        reviewPageQueryDTO.setStatus(1);
        return Result.success(reviewService.pageQuery(reviewPageQueryDTO));
    }
}

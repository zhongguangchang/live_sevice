package com.sky.controller.admin;

import com.sky.dto.ReviewPageQueryDTO;
import com.sky.dto.ReviewReplyDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ReviewService;
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
 * 评价管理（管理端）
 */
@RestController("adminReviewController")
@RequestMapping("/admin/review")
@Api(tags = "评价管理接口")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/page")
    @ApiOperation("评价分页查询")
    public Result<PageResult> page(ReviewPageQueryDTO reviewPageQueryDTO) {
        return Result.success(reviewService.pageQuery(reviewPageQueryDTO));
    }

    @PutMapping("/reply")
    @ApiOperation("回复评价")
    public Result reply(@RequestBody ReviewReplyDTO reviewReplyDTO) {
        log.info("回复评价：{}", reviewReplyDTO);
        reviewService.reply(reviewReplyDTO);
        return Result.success();
    }

    /**
     * 显示 / 隐藏评价，运营处理恶意评价用
     */
    @PostMapping("/status/{status}")
    @ApiOperation("显示或隐藏评价")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改评价状态：id={}, status={}", id, status);
        reviewService.updateStatus(id, status);
        return Result.success();
    }
}

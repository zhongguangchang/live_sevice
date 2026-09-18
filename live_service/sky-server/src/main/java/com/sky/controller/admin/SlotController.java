package com.sky.controller.admin;

import com.sky.dto.SlotBatchCreateDTO;
import com.sky.dto.SlotQueryDTO;
import com.sky.result.Result;
import com.sky.service.SlotService;
import com.sky.vo.SlotVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务时段排期管理（管理端）
 */
@RestController("adminSlotController")
@RequestMapping("/admin/slot")
@Api(tags = "服务时段排期接口")
@Slf4j
public class SlotController {

    @Autowired
    private SlotService slotService;

    /**
     * 批量生成排期。
     * <p>
     * 入参是「日期区间 x 师傅列表 x 服务列表 x 时段列表」的笛卡尔积，
     * 生成完会自动做一次缓存预热，避免第一个下单的用户撞上库存未初始化。
     */
    @PostMapping("/batch")
    @ApiOperation("批量生成排期")
    public Result batchCreate(@RequestBody SlotBatchCreateDTO slotBatchCreateDTO) {
        log.info("批量生成排期：{}", slotBatchCreateDTO);
        slotService.batchCreate(slotBatchCreateDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("条件查询排期")
    public Result<List<SlotVO>> list(SlotQueryDTO slotQueryDTO) {
        List<SlotVO> list = slotService.listByCondition(slotQueryDTO);
        return Result.success(list);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("关闭或开放时段")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改时段状态：id={}, status={}", id, status);
        slotService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 手动触发缓存预热，管理端运营可以用它在开售前把库存刷进 Redis
     */
    @PostMapping("/warmup")
    @ApiOperation("手动预热排期库存到 Redis")
    public Result<Integer> warmUp(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        int count = slotService.warmUpStock(begin, end);
        return Result.success(count);
    }

    /**
     * 手动触发库存对账。正常情况下由定时任务每 10 分钟跑一次，
     * 这里暴露给运营在发现数据异常时立即修正。
     */
    @PostMapping("/reconcile")
    @ApiOperation("手动触发库存对账")
    public Result<Integer> reconcile() {
        int fixed = slotService.reconcile();
        return Result.success(fixed);
    }
}

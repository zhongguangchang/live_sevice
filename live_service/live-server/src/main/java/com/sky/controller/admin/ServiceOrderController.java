package com.sky.controller.admin;

import com.sky.dto.ServiceOrderCancelDTO;
import com.sky.dto.ServiceOrderDispatchDTO;
import com.sky.dto.ServiceOrderPageQueryDTO;
import com.sky.dto.ServiceOrderRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ServiceOrderService;
import com.sky.vo.ServiceOrderStatisticsVO;
import com.sky.vo.ServiceOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务订单管理（管理端）
 */
@RestController("adminServiceOrderController")
@RequestMapping("/admin/serviceOrder")
@Api(tags = "服务订单管理接口")
@Slf4j
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("订单条件搜索")
    public Result<PageResult> conditionSearch(ServiceOrderPageQueryDTO serviceOrderPageQueryDTO) {
        PageResult pageResult = serviceOrderService.pageQuery(serviceOrderPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    @ApiOperation("各状态订单数量统计")
    public Result<ServiceOrderStatisticsVO> statistics() {
        return Result.success(serviceOrderService.statistics());
    }

    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result<ServiceOrderVO> details(@PathVariable Long id) {
        return Result.success(serviceOrderService.getDetail(id));
    }

    /**
     * 派单。providerId 为空时走自动派单算法：
     * 技能过滤 -> 状态过滤 -> 评分排序 -> 档期过滤。
     */
    @PutMapping("/dispatch")
    @ApiOperation("派单给服务人员")
    public Result dispatch(@RequestBody ServiceOrderDispatchDTO serviceOrderDispatchDTO) {
        log.info("派单：{}", serviceOrderDispatchDTO);
        serviceOrderService.dispatch(serviceOrderDispatchDTO);
        return Result.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody ServiceOrderCancelDTO serviceOrderCancelDTO) {
        log.info("管理端取消订单：{}", serviceOrderCancelDTO);
        serviceOrderService.userCancel(serviceOrderCancelDTO.getId());
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {
        log.info("完成订单：id={}", id);
        serviceOrderService.completeService(id, null);
        return Result.success();
    }

    /**
     * 接单
     * <p>
     * 正常场景是师傅在自己的端上接单，这里放在管理端有两个用途：
     * 一是演示时不用真的开一个师傅端就能走通全流程，
     * 二是师傅长时间联系不上时运营可以代接单兜底。
     * 订单的 provider_id 在派单时已经写好了，这里不用再传。
     */
    @PutMapping("/accept/{id}")
    @ApiOperation("接单")
    public Result accept(@PathVariable Long id) {
        log.info("接单：orderId={}", id);
        serviceOrderService.accept(id, null);
        return Result.success();
    }

    /**
     * 拒单。订单会退回「待接单」状态，由运营重新派单
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody ServiceOrderRejectionDTO serviceOrderRejectionDTO) {
        log.info("拒单：{}", serviceOrderRejectionDTO);
        serviceOrderService.reject(serviceOrderRejectionDTO);
        return Result.success();
    }

    /**
     * 师傅到达现场，开始服务
     */
    @PutMapping("/start/{id}")
    @ApiOperation("开始服务")
    public Result startService(@PathVariable Long id) {
        log.info("开始服务：orderId={}", id);
        serviceOrderService.startService(id, null);
        return Result.success();
    }

    /**
     * 到店服务的核销
     * <p>
     * 用户到店后出示订单里的 6 位核销码，门店或师傅核对后在这里核销，
     * 核销即视为服务开始。
     */
    @PutMapping("/verify")
    @ApiOperation("到店核销")
    public Result verify(@RequestParam Long orderId, @RequestParam String verifyCode) {
        log.info("到店核销：orderId={}", orderId);
        serviceOrderService.verify(orderId, verifyCode);
        return Result.success();
    }
}

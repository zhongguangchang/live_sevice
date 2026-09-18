package com.sky.controller.admin;

import com.sky.dto.ServiceAreaDTO;
import com.sky.entity.ServiceArea;
import com.sky.result.Result;
import com.sky.service.ServiceAreaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务区域管理（管理端）
 * <p>
 * 平台不是哪儿都上门，这张表定义了业务覆盖的行政区划。
 * 用户下单时用地址的区级编号匹配，匹配不到就提示暂未开通。
 */
@RestController("adminServiceAreaController")
@RequestMapping("/admin/serviceArea")
@Api(tags = "服务区域接口")
@Slf4j
public class ServiceAreaController {

    @Autowired
    private ServiceAreaService serviceAreaService;

    @PostMapping
    @ApiOperation("新增服务区域")
    public Result save(@RequestBody ServiceAreaDTO serviceAreaDTO) {
        log.info("新增服务区域：{}", serviceAreaDTO);
        serviceAreaService.save(serviceAreaDTO);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改服务区域")
    public Result update(@RequestBody ServiceAreaDTO serviceAreaDTO) {
        log.info("修改服务区域：{}", serviceAreaDTO);
        serviceAreaService.update(serviceAreaDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除服务区域")
    public Result delete(@PathVariable Long id) {
        log.info("删除服务区域：id={}", id);
        serviceAreaService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查询服务区域列表")
    public Result<List<ServiceArea>> list(ServiceArea serviceArea) {
        return Result.success(serviceAreaService.list(serviceArea));
    }

    @PostMapping("/status/{status}")
    @ApiOperation("开通或停用服务区域")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改服务区域状态：id={}, status={}", id, status);
        serviceAreaService.updateStatus(id, status);
        return Result.success();
    }
}

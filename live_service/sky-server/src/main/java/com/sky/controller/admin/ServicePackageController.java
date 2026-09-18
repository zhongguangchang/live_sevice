package com.sky.controller.admin;

import com.sky.dto.ServicePackageDTO;
import com.sky.dto.ServicePackagePageQueryDTO;
import com.sky.entity.ServicePackage;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ServicePackageService;
import com.sky.vo.ServicePackageVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务套餐管理（管理端）
 */
@RestController("adminServicePackageController")
@RequestMapping("/admin/servicePackage")
@Api(tags = "服务套餐相关接口")
@Slf4j
public class ServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @PostMapping
    @ApiOperation("新增服务套餐")
    public Result save(@RequestBody ServicePackageDTO servicePackageDTO) {
        log.info("新增服务套餐：{}", servicePackageDTO);
        servicePackageService.saveWithItems(servicePackageDTO);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改服务套餐")
    public Result update(@RequestBody ServicePackageDTO servicePackageDTO) {
        log.info("修改服务套餐：{}", servicePackageDTO);
        servicePackageService.updateWithItems(servicePackageDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("服务套餐分页查询")
    public Result<PageResult> page(ServicePackagePageQueryDTO servicePackagePageQueryDTO) {
        PageResult pageResult = servicePackageService.pageQuery(servicePackagePageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询服务套餐")
    public Result<ServicePackageVO> getById(@PathVariable Long id) {
        ServicePackageVO vo = servicePackageService.getByIdWithItems(id);
        return Result.success(vo);
    }

    @DeleteMapping
    @ApiOperation("批量删除服务套餐")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除服务套餐：{}", ids);
        servicePackageService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("服务套餐启用停用")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("服务套餐启用停用：status={}, id={}", status, id);
        servicePackageService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类查询服务套餐")
    public Result<List<ServicePackage>> list(ServicePackage servicePackage) {
        List<ServicePackage> list = servicePackageService.list(servicePackage);
        return Result.success(list);
    }
}

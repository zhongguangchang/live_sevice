package com.sky.controller.admin;

import com.sky.dto.ServiceItemDTO;
import com.sky.dto.ServiceItemPageQueryDTO;
import com.sky.entity.ServiceItem;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ServiceItemService;
import com.sky.vo.ServiceItemVO;
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
 * 服务项目管理（管理端）
 */
@RestController("adminServiceItemController")
@RequestMapping("/admin/serviceItem")
@Api(tags = "服务项目相关接口")
@Slf4j
public class ServiceItemController {

    @Autowired
    private ServiceItemService serviceItemService;

    @PostMapping
    @ApiOperation("新增服务项目")
    public Result save(@RequestBody ServiceItemDTO serviceItemDTO) {
        log.info("新增服务项目：{}", serviceItemDTO);
        serviceItemService.saveWithSpecs(serviceItemDTO);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改服务项目")
    public Result update(@RequestBody ServiceItemDTO serviceItemDTO) {
        log.info("修改服务项目：{}", serviceItemDTO);
        serviceItemService.updateWithSpecs(serviceItemDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("服务项目分页查询")
    public Result<PageResult> page(ServiceItemPageQueryDTO serviceItemPageQueryDTO) {
        PageResult pageResult = serviceItemService.pageQuery(serviceItemPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询服务项目")
    public Result<ServiceItemVO> getById(@PathVariable Long id) {
        ServiceItemVO vo = serviceItemService.getByIdWithSpecs(id);
        return Result.success(vo);
    }

    /**
     * 批量删除。ids 用查询参数传，形如 ?ids=1,2,3
     */
    @DeleteMapping
    @ApiOperation("批量删除服务项目")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("批量删除服务项目：{}", ids);
        serviceItemService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("服务项目起售停售")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("服务项目起售停售：status={}, id={}", status, id);
        serviceItemService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类查询服务项目")
    public Result<List<ServiceItem>> list(ServiceItem serviceItem) {
        List<ServiceItem> list = serviceItemService.list(serviceItem);
        return Result.success(list);
    }
}

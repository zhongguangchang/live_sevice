package com.sky.controller.admin;

import com.sky.dto.ProviderDTO;
import com.sky.dto.ProviderPageQueryDTO;
import com.sky.entity.Provider;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ProviderService;
import com.sky.vo.ProviderVO;
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

import java.util.List;

/**
 * 服务人员（师傅）管理（管理端）
 */
@RestController("adminProviderController")
@RequestMapping("/admin/provider")
@Api(tags = "服务人员相关接口")
@Slf4j
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @PostMapping
    @ApiOperation("新增服务人员")
    public Result save(@RequestBody ProviderDTO providerDTO) {
        log.info("新增服务人员：{}", providerDTO);
        providerService.saveWithSkills(providerDTO);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改服务人员")
    public Result update(@RequestBody ProviderDTO providerDTO) {
        log.info("修改服务人员：{}", providerDTO);
        providerService.updateWithSkills(providerDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("服务人员分页查询")
    public Result<PageResult> page(ProviderPageQueryDTO providerPageQueryDTO) {
        PageResult pageResult = providerService.pageQuery(providerPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询服务人员")
    public Result<ProviderVO> getById(@PathVariable Long id) {
        ProviderVO vo = providerService.getByIdWithSkills(id);
        return Result.success(vo);
    }

    /**
     * 接单状态 1可接单 2忙碌 3休息中 4已离职
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改服务人员接单状态")
    public Result updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改师傅接单状态：id={}, status={}", id, status);
        providerService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查询服务人员列表")
    public Result<List<Provider>> list(Provider provider) {
        List<Provider> list = providerService.list(provider);
        return Result.success(list);
    }

    @GetMapping("/available")
    @ApiOperation("按技能分类查询可接单的师傅")
    public Result<List<ProviderVO>> listAvailable(Long categoryId) {
        List<ProviderVO> list = providerService.listAvailable(categoryId);
        return Result.success(list);
    }
}

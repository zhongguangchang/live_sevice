package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ServiceItemService;
import com.sky.vo.ServiceItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务项目浏览（用户端）
 * <p>
 * 只暴露已起售的服务，停售的查不到。
 */
@RestController("userServiceItemController")
@RequestMapping("/user/service")
@Api(tags = "用户端服务项目接口")
@Slf4j
public class ServiceItemController {

    @Autowired
    private ServiceItemService serviceItemService;

    @GetMapping("/list")
    @ApiOperation("根据分类查询已起售的服务项目")
    public Result<List<ServiceItemVO>> list(Long categoryId) {
        log.info("用户端查询服务项目列表：categoryId={}", categoryId);
        List<ServiceItemVO> list = serviceItemService.listByCategoryId(categoryId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询服务项目详情")
    public Result<ServiceItemVO> detail(@PathVariable Long id) {
        ServiceItemVO vo = serviceItemService.getByIdWithSpecs(id);
        return Result.success(vo);
    }
}

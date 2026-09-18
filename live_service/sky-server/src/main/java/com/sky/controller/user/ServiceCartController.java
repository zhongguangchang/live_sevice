package com.sky.controller.user;

import com.sky.dto.ServiceCartDTO;
import com.sky.entity.ServiceCart;
import com.sky.result.Result;
import com.sky.service.ServiceCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务清单（用户端）
 * <p>
 * 生活服务不叫购物车，叫「待预约清单」更贴切 ——
 * 用户先把几个服务放进来，最后一起选时段提交成一个预约单。
 */
@RestController("userServiceCartController")
@RequestMapping("/user/cart")
@Api(tags = "用户端服务清单接口")
@Slf4j
public class ServiceCartController {

    @Autowired
    private ServiceCartService serviceCartService;

    @PostMapping("/add")
    @ApiOperation("加入服务清单")
    public Result add(@RequestBody ServiceCartDTO serviceCartDTO) {
        log.info("加入服务清单：{}", serviceCartDTO);
        serviceCartService.add(serviceCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查看服务清单")
    public Result<List<ServiceCart>> list() {
        return Result.success(serviceCartService.list());
    }

    @PostMapping("/sub")
    @ApiOperation("从清单中减去一个")
    public Result sub(@RequestBody ServiceCartDTO serviceCartDTO) {
        serviceCartService.sub(serviceCartDTO);
        return Result.success();
    }

    @DeleteMapping("/clean")
    @ApiOperation("清空服务清单")
    public Result clean() {
        serviceCartService.clean();
        return Result.success();
    }
}

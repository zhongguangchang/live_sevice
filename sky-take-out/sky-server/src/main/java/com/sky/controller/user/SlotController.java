package com.sky.controller.user;

import com.sky.dto.SlotQueryDTO;
import com.sky.result.Result;
import com.sky.service.SlotService;
import com.sky.vo.SlotVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可预约时段查询（用户端）
 */
@RestController("userSlotController")
@RequestMapping("/user/slot")
@Api(tags = "用户端时段接口")
@Slf4j
public class SlotController {

    @Autowired
    private SlotService slotService;

    /**
     * 查某个服务某天还有哪些时段可以约。
     * <p>
     * 返回的 remainStock 是实时剩余名额，注意它读的是 Redis 而不是数据库
     * 因为此刻可能有别人刚下单但还没付款，数据库那边还没落账。
     */
    @GetMapping("/available")
    @ApiOperation("查询可预约时段")
    public Result<List<SlotVO>> available(SlotQueryDTO slotQueryDTO) {
        log.info("查询可预约时段：{}", slotQueryDTO);
        List<SlotVO> list = slotService.listAvailable(slotQueryDTO);
        return Result.success(list);
    }
}

package com.sqx.app.rental.stock.controller;

import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户端-租赁库存
 */
@Tag(name = "用户端-租赁库存", description = "租赁时段库存校验")
@RestController
@RequestMapping("/app/rental/stock")
public class AppRentStockController {

    @Resource
    private RentStockService rentStockService;

    @Operation(summary = "时段库存校验", description = "按租期时间段校验可用库存")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentStockCheckVo.class)))
    @PostMapping("/check")
    public Result<RentStockCheckVo> check(@RequestBody @Validated RentStockCheckBo checkBo) {
        return Result.success(rentStockService.check(checkBo));
    }
}

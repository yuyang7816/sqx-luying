package com.sqx.app.rental.stock.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockAdjustBo;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockPageBo;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.AdminRentStockListVo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.service.AdminRentStockService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端-租赁库存
 */
@Tag(name = "管理端-租赁库存", description = "库存分页/占用查询/手动调整")
@RestController
@RequestMapping("/admin/rental/stock")
public class AdminRentStockController {

    @Resource
    private AdminRentStockService adminRentStockService;

    @Operation(summary = "库存分页")
    @PostMapping("/page")
    public Result<IPage<AdminRentStockListVo>> page(@RequestBody @Validated AdminRentStockPageBo pageBo) {
        return Result.success(adminRentStockService.page(pageBo));
    }

    @Operation(summary = "时段占用查询")
    @PostMapping("/occupy/query")
    public Result<RentStockCheckVo> occupyQuery(@RequestBody @Validated RentStockCheckBo checkBo) {
        return Result.success(adminRentStockService.occupyQuery(checkBo));
    }

    @Operation(summary = "手动调整库存")
    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody @Validated AdminRentStockAdjustBo adjustBo) {
        adminRentStockService.adjust(adjustBo);
        return Result.success();
    }
}

package com.sqx.app.rental.stock.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockAdjustBo;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockPageBo;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.AdminRentStockListVo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;

/**
 * 管理端库存服务
 */
public interface AdminRentStockService {

    IPage<AdminRentStockListVo> page(AdminRentStockPageBo pageBo);

    RentStockCheckVo occupyQuery(RentStockCheckBo checkBo);

    void adjust(AdminRentStockAdjustBo adjustBo);
}

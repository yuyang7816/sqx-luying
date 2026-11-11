package com.sqx.app.rental.order.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端-验收记录分页查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "验收记录分页查询")
public class RentInspectionPageBo extends PageParam {

    @Schema(description = "关键词：订单号")
    private String keyword;

    @Schema(description = "复核状态：pending/approved/rejected")
    private String auditStatus;

    @Schema(description = "设备状况：good/minor_damage/major_damage")
    private String deviceCondition;
}

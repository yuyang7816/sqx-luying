package com.sqx.app.merchant.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 联系用户 VO
 */
@Data
@Schema(description = "联系用户")
public class MerchantOrderContactVo {

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人手机号")
    private String contactPhone;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "用户ID（IM 联系）")
    private Long userId;
}

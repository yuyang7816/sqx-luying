package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 用户地址保存/更新
 */
@Data
@Schema(description = "用户地址保存/更新")
public class UserAddressSaveBo {

    @Schema(description = "地址ID，更新时必填")
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名")
    private String contactName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "收货人手机号")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址")
    private String detailAddress;

    @Schema(description = "是否设为默认：true/false")
    private Boolean isDefault;
}

package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户配送地址 VO
 */
@Data
@Schema(description = "用户配送地址")
public class UserAddressVo {

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "收货人姓名")
    private String contactName;

    @Schema(description = "收货人手机号")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    private String detailAddress;

    @Schema(description = "完整地址（省市区+详细地址）")
    private String fullAddress;

    @Schema(description = "是否默认：1是 0否")
    private Integer isDefault;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

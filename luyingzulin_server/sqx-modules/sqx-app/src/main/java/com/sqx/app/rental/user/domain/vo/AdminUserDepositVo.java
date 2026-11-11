package com.sqx.app.rental.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminUserDepositVo {

    private Long id;
    private Long userId;
    private String phoneMasked;
    private String phone;
    private String userName;
    private BigDecimal totalAmount;
    private BigDecimal frozenAmount;
    private BigDecimal availableAmount;
    private Integer status;
}

package com.sqx.app.merchant.wallet.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantSecurityDepositPayBo {

    /** 兼容旧客户端传参，实际金额以系统配置为准 */
    private BigDecimal amount;

    private String remark;
}

package com.sqx.app.merchant.wallet.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "商户保证金流水")
public class MerchantSecurityDepositLogVo {

    private Long id;
    private String logNo;
    private String logType;
    private String logTypeLabel;
    private BigDecimal amount;
    private String orderNo;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

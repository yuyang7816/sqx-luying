package com.sqx.app.merchant.wallet.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sqx.app.merchant.wallet.domain.entity.MerchantWalletTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantWalletTransactionVo extends MerchantWalletTransaction {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Override
    public java.time.LocalDateTime getCreateTime() {
        return super.getCreateTime();
    }
}

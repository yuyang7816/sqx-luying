package com.sqx.app.merchant.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.merchant.wallet.domain.entity.MerchantDeposit;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface MerchantDepositMapper extends BaseMapper<MerchantDeposit> {

    void incrTotal(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    void decrTotal(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    int freezeAvailable(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    int unfreezeAvailable(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    int refundFrozen(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);
}

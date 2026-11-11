package com.sqx.app.merchant.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.merchant.wallet.domain.entity.MerchantWallet;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface MerchantWalletMapper extends BaseMapper<MerchantWallet> {

    void incrAmount(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);

    void decrAmount(@Param("merchantId") Long merchantId, @Param("amount") BigDecimal amount);
}

package com.sqx.app.merchant.wallet.domain.vo;

import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商户钱包概览")
public class MerchantWalletSummaryVo {

    @Schema(description = "商户经营钱包")
    private MerchantWalletVo wallet;

    @Schema(description = "商户保证金概览")
    private MerchantSecurityDepositSummaryVo securityDeposit;

    @Schema(description = "最近经营流水")
    private List<MerchantWalletTransactionVo> recentTransactions;

    @Schema(description = "最近提现记录")
    private List<WithdrawRecordVo> recentWithdraws;
}

package com.sqx.app.wallet.wallet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.wallet.wallet.domain.bo.*;
import com.sqx.app.wallet.wallet.domain.vo.UserWalletRechargePayStatusVo;
import com.sqx.app.wallet.wallet.domain.vo.UserWalletVo;

import java.math.BigDecimal;

/**
 * 用户钱包主表 服务类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-13 15:09:58
 */
public interface UserWalletService {

    void saveInfo(UserWalletSaveBo userWalletSaveBo);

    IPage<UserWalletVo> pageInfo(UserWalletPageBo userWalletPageBo);

    UserWalletVo info(UserWalletInfoBo userWalletInfoBo);

    void updateInfo(UserWalletUpdateBo userWalletUpdateBo);

    void delete(UserWalletInfoBo userWalletInfoBo);

    UserWalletVo wallet();

    void updateAmount(UserWalletUpdateAmountBo userWalletUpdateAmountBo);

    void payWithBalance(UserWalletPayBalanceBo bo);

    String applyRecharge(Long userId, UserWalletRechargeBo rechargeBo);

    BigDecimal prepareRechargePay(Long userId, String payNo, BigDecimal amount);

    UserWalletRechargePayStatusVo rechargePayStatus(Long userId, String payNo);

    void onRechargePaySuccess(Long userId, String payNo, BigDecimal amount);
}

package com.sqx.app.referral.withdraw.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawalAccountInfoBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawalAccountPageBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawalAccountSaveBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawalAccountUpdateBo;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawalAccount;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawalAccountVo;

/**
 * 提现账号 服务类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-06-20 14:02:39
 */
public interface WithdrawalAccountService {

    void saveInfo(WithdrawalAccountSaveBo withdrawalAccountSaveBo);

    IPage<WithdrawalAccountVo> pageInfo(WithdrawalAccountPageBo withdrawalAccountPageBo);

    WithdrawalAccountVo info(WithdrawalAccountInfoBo withdrawalAccountInfoBo);

    void updateInfo(WithdrawalAccountUpdateBo withdrawalAccountUpdateBo);

    void delete(WithdrawalAccountInfoBo withdrawalAccountInfoBo);

    WithdrawalAccount getUserWithdrawalAccount(Long userId, Integer type);
}

package com.sqx.app.referral.withdraw.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.referral.withdraw.domain.bo.*;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;

/**
 * 提现记录 服务类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-05 10:10:32
 */
public interface WithdrawRecordService {

    void saveInfo(WithdrawRecordSaveBo withdrawRecordSaveBo);

    IPage<WithdrawRecordVo> pageInfo(WithdrawRecordPageBo withdrawRecordPageBo);

    WithdrawRecordVo info(WithdrawRecordInfoBo withdrawRecordInfoBo);

    void updateInfo(WithdrawRecordUpdateBo withdrawRecordUpdateBo);

    void delete(WithdrawRecordInfoBo withdrawRecordInfoBo);

    void confirm(WithdrawRecordConfirmBo withdrawRecordConfirmBo);
}

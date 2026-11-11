package com.sqx.app.merchant.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyAuditBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyPageBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplySubmitBo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyListVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyProgressVo;

public interface MerchantApplyService {

    void submit(Long userId, MerchantApplySubmitBo submitBo);

    MerchantApplyProgressVo progress(Long userId);

    IPage<MerchantApplyListVo> adminPage(MerchantApplyPageBo pageBo);

    void audit(Long adminId, MerchantApplyAuditBo auditBo);
}

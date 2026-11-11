package com.sqx.app.rental.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.order.domain.bo.RentInspectionAuditBo;
import com.sqx.app.rental.order.domain.bo.RentInspectionPageBo;
import com.sqx.app.rental.order.domain.bo.RentInspectionSubmitBo;
import com.sqx.app.rental.order.domain.vo.RentInspectionDetailVo;
import com.sqx.app.rental.order.domain.vo.RentInspectionListVo;

/**
 * 验收服务
 */
public interface RentInspectionService {

    /**
     * 商户录入验收（returning/inspecting → completed + 退押）
     */
    void merchantSubmit(Long operatorId, RentInspectionSubmitBo submitBo);

    /**
     * 管理端验收分页
     */
    IPage<RentInspectionListVo> adminPage(RentInspectionPageBo pageBo);

    /**
     * 管理端验收详情
     */
    RentInspectionDetailVo adminDetail(Long id);

    /**
     * 管理端验收复核
     */
    void adminAudit(RentInspectionAuditBo auditBo);
}

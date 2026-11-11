package com.sqx.app.rental.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.CouponGrantBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplatePageBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplateSaveBo;
import com.sqx.app.rental.user.domain.vo.CouponTemplateVo;

public interface CouponTemplateService {

    IPage<CouponTemplateVo> page(CouponTemplatePageBo pageBo);

    CouponTemplateVo info(Long id);

    void save(CouponTemplateSaveBo saveBo);

    void update(CouponTemplateSaveBo saveBo);

    void delete(Long id);

    int grant(CouponGrantBo grantBo);
}

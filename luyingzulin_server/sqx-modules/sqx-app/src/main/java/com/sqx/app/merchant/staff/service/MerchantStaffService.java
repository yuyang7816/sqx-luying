package com.sqx.app.merchant.staff.service;

import com.sqx.app.merchant.staff.domain.bo.MerchantStaffSaveBo;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffProfileVo;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffVo;

import java.util.List;

public interface MerchantStaffService {

    List<MerchantStaffVo> list(Long userId);

    MerchantStaffProfileVo profile(Long userId);

    void save(Long userId, MerchantStaffSaveBo saveBo);

    void delete(Long userId, Long staffId);
}

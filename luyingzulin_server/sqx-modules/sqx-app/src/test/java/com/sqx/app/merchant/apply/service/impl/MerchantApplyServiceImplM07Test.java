package com.sqx.app.merchant.apply.service.impl;

import com.sqx.app.merchant.apply.domain.bo.MerchantApplyAuditBo;
import com.sqx.app.merchant.apply.domain.entity.MerchantApply;
import com.sqx.app.merchant.apply.mapper.MerchantApplyMapper;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.enums.ApplyStatus;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantApplyServiceImplM07Test {

    @InjectMocks
    private MerchantApplyServiceImpl merchantApplyService;

    @Mock
    private MerchantApplyMapper merchantApplyMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private MerchantStoreMapper merchantStoreMapper;

    @Test
    void auditApprovedShouldCreateMerchantAndStore() {
        MerchantApply apply = new MerchantApply();
        apply.setId(1L);
        apply.setUserId(10L);
        apply.setMerchantName("测试商户");
        apply.setContactName("王五");
        apply.setContactPhone("13900001001");
        apply.setStoreAddress("西安市高新区");
        apply.setStatus(ApplyStatus.REVIEWING.getCode());

        when(merchantApplyMapper.selectById(1L)).thenReturn(apply);
        when(merchantMapper.selectOne(any())).thenReturn(null);

        MerchantApplyAuditBo auditBo = new MerchantApplyAuditBo();
        auditBo.setApplyId(1L);
        auditBo.setAuditResult("approved");

        merchantApplyService.audit(99L, auditBo);

        verify(merchantMapper).insert(any());
        verify(merchantStoreMapper).insert(any());
        verify(merchantApplyMapper).updateById(any());
        assertEquals(ApplyStatus.APPROVED.getCode(), apply.getStatus());
    }
}

package com.sqx.app.merchant.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.enums.MerchantStatus;
import com.sqx.app.merchant.staff.domain.entity.MerchantStaff;
import com.sqx.app.merchant.staff.mapper.MerchantStaffMapper;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.common.core.exception.BizException;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 商户上下文：解析当前登录用户所属商户
 */
@Component
public class MerchantContextSupport {

    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantStaffMapper merchantStaffMapper;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;

    public MerchantContext requireMerchantContext(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant != null) {
            assertActive(merchant);
            return new MerchantContext(merchant.getId(), merchant.getUserId(), null);
        }

        MerchantStaff staff = merchantStaffMapper.selectOne(new LambdaQueryWrapper<MerchantStaff>()
                .eq(MerchantStaff::getUserId, userId)
                .eq(MerchantStaff::getStatus, 1)
                .last("LIMIT 1"));
        if (staff != null) {
            merchant = merchantMapper.selectById(staff.getMerchantId());
            if (merchant == null) {
                throw new BizException("商户不存在");
            }
            assertActive(merchant);
            return new MerchantContext(merchant.getId(), merchant.getUserId(), staff.getStoreId());
        }

        throw new BizException("您尚未开通商户权限，请先完成入驻审核");
    }

    /**
     * 经营操作上下文：已通过入驻且已缴足保证金（若系统配置要求）
     */
    public MerchantContext requireMerchantContextForOperation(Long userId) {
        MerchantContext context = requireMerchantContext(userId);
        merchantSecurityDepositService.assertDepositPaidIfRequired(context.getMerchantId());
        return context;
    }

    public Merchant requireMerchant(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant == null) {
            throw new BizException("您尚未开通商户权限");
        }
        assertActive(merchant);
        return merchant;
    }

    private void assertActive(Merchant merchant) {
        if (!MerchantStatus.ACTIVE.getCode().equals(merchant.getStatus())) {
            throw new BizException("商户账号已冻结，请联系平台客服");
        }
    }

    @Getter
    public static class MerchantContext {
        private final Long merchantId;
        private final Long ownerUserId;
        private final Long storeId;

        public MerchantContext(Long merchantId, Long ownerUserId, Long storeId) {
            this.merchantId = merchantId;
            this.ownerUserId = ownerUserId;
            this.storeId = storeId;
        }
    }
}

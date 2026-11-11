package com.sqx.app.merchant.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.merchant.wallet.domain.bo.MerchantWalletTransactionPageBo;
import com.sqx.app.merchant.wallet.domain.entity.MerchantWallet;
import com.sqx.app.merchant.wallet.domain.entity.MerchantWalletTransaction;
import com.sqx.app.merchant.wallet.domain.vo.MerchantWalletTransactionVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantWalletVo;
import com.sqx.app.merchant.wallet.mapper.MerchantWalletMapper;
import com.sqx.app.merchant.wallet.mapper.MerchantWalletTransactionMapper;
import com.sqx.app.merchant.wallet.service.MerchantWalletAccountService;
import com.sqx.app.referral.constants.ReferralConstant;
import com.sqx.app.referral.withdraw.db.service.IWithdrawRecordService;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawRecord;
import com.sqx.app.referral.withdraw.enums.WithdrawSourceType;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.system.db.service.ISysConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MerchantWalletAccountServiceImpl implements MerchantWalletAccountService {

    private static final int TX_TYPE_WITHDRAW = 10;
    private static final int TX_STATUS_COMPLETED = 2;
    private static final int DIRECTION_OUT = 2;

    @Resource
    private MerchantWalletMapper merchantWalletMapper;
    @Resource
    private MerchantWalletTransactionMapper merchantWalletTransactionMapper;
    @Resource
    private IWithdrawRecordService iWithdrawRecordService;
    @Resource
    private ISysConfigService iSysConfigService;

    @Override
    public MerchantWalletVo summary(Long merchantId) {
        MerchantWallet wallet = ensureWallet(merchantId);
        MerchantWalletVo vo = new MerchantWalletVo();
        vo.setMerchantId(merchantId);
        vo.setAmount(nullToZero(wallet.getAmount()));
        vo.setFrozenAmount(nullToZero(wallet.getFrozenAmount()));
        return vo;
    }

    @Override
    public IPage<MerchantWalletTransactionVo> transactionPage(Long merchantId, MerchantWalletTransactionPageBo pageBo) {
        Page<MerchantWalletTransaction> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        IPage<MerchantWalletTransaction> txPage = merchantWalletTransactionMapper.selectPage(page,
                new LambdaQueryWrapper<MerchantWalletTransaction>()
                        .eq(MerchantWalletTransaction::getMerchantId, merchantId)
                        .orderByDesc(MerchantWalletTransaction::getCreateTime));
        List<MerchantWalletTransactionVo> records = new ArrayList<>();
        for (MerchantWalletTransaction record : txPage.getRecords()) {
            MerchantWalletTransactionVo vo = new MerchantWalletTransactionVo();
            BeanUtils.copyProperties(record, vo);
            records.add(vo);
        }
        return PageUtil.copyPage(txPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long merchantId, Long ownerUserId, WithdrawRecordSaveBo saveBo) {
        MerchantWallet wallet = ensureWallet(merchantId);
        if (saveBo.getAmount() == null
                || nullToZero(wallet.getAmount()).compareTo(saveBo.getAmount()) < 0) {
            throw new BizException("商户钱包余额不足");
        }
        validateWithdrawAmount(saveBo.getAmount());

        String serviceFeeRate = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_RATE);
        BigDecimal serviceFee = saveBo.getAmount().multiply(new BigDecimal(serviceFeeRate));
        BigDecimal actualAmount = saveBo.getAmount().subtract(serviceFee);

        WithdrawRecord withdrawRecord = new WithdrawRecord();
        BeanUtils.copyProperties(saveBo, withdrawRecord);
        withdrawRecord.setUserId(ownerUserId);
        withdrawRecord.setSourceType(WithdrawSourceType.MERCHANT_WALLET.getCode());
        withdrawRecord.setActualAmount(actualAmount);
        withdrawRecord.setServiceFee(serviceFee);
        withdrawRecord.setStatus(1);
        iWithdrawRecordService.save(withdrawRecord);

        merchantWalletMapper.decrAmount(merchantId, saveBo.getAmount());
        recordTransaction(merchantId, TX_TYPE_WITHDRAW, DIRECTION_OUT, TX_TYPE_WITHDRAW,
                withdrawRecord.getId(), saveBo.getAmount(), "商户钱包提现", String.valueOf(withdrawRecord.getId()));
    }

    @Override
    public void recordTransaction(Long merchantId, Integer transactionType, Integer direction, Integer bizType,
                                  Long bizId, BigDecimal amount, String title, String description) {
        MerchantWalletTransaction tx = new MerchantWalletTransaction();
        tx.setMerchantId(merchantId);
        tx.setTransactionType(transactionType);
        tx.setDirection(direction);
        tx.setBizType(bizType);
        tx.setBizId(bizId);
        tx.setAmount(amount);
        tx.setStatus(TX_STATUS_COMPLETED);
        tx.setTitle(title);
        tx.setDescription(description);
        merchantWalletTransactionMapper.insert(tx);
    }

    @Override
    public void refundWithdraw(Long merchantId, BigDecimal amount) {
        merchantWalletMapper.incrAmount(merchantId, amount);
    }

    private MerchantWallet ensureWallet(Long merchantId) {
        MerchantWallet wallet = merchantWalletMapper.selectOne(new LambdaQueryWrapper<MerchantWallet>()
                .eq(MerchantWallet::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (wallet != null) {
            return wallet;
        }
        wallet = new MerchantWallet();
        wallet.setMerchantId(merchantId);
        wallet.setAmount(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        merchantWalletMapper.insert(wallet);
        return wallet;
    }

    private void validateWithdrawAmount(BigDecimal amount) {
        String maxAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MAX);
        if (amount.compareTo(new BigDecimal(maxAmount)) > 0) {
            throw new BizException("超过单次提现上限");
        }
        String minAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MIN);
        if (amount.compareTo(new BigDecimal(minAmount)) < 0) {
            throw new BizException("低于单次提现下限");
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

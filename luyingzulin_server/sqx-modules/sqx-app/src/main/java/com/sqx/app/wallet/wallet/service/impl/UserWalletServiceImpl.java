package com.sqx.app.wallet.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.pay.business.service.BusinessService;
import com.sqx.app.base.pay.domain.BusinessParam;
import com.sqx.app.base.pay.enums.BusinessType;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.app.rental.order.service.RentRenewService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.bo.*;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.app.wallet.wallet.domain.vo.UserWalletRechargePayStatusVo;
import com.sqx.app.wallet.wallet.domain.vo.UserWalletVo;
import com.sqx.app.wallet.wallet.enums.Operator;
import com.sqx.app.wallet.wallet.enums.WalletBizType;
import com.sqx.app.wallet.wallet.service.UserWalletService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.common.shiro.utils.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户钱包主表 服务实现类
 */
@Slf4j
@Service
public class UserWalletServiceImpl implements UserWalletService {

    private static final String PAY_CHANNEL_BALANCE = "balance";
    private static final String RECHARGE_PAY_NO_PREFIX = "WR";
    private static final int TX_STATUS_PENDING = 1;
    private static final int TX_STATUS_COMPLETED = 2;
    private static final int TX_TYPE_RECHARGE = 1;

    @Resource
    private IUserWalletService iUserWalletService;
    @Resource
    private IUserWalletTransactionService iUserWalletTransactionService;
    @Lazy
    @Resource
    private BusinessService businessService;
    @Resource
    private RentOrderService rentOrderService;
    @Resource
    private RentRenewService rentRenewService;
    @Resource
    private UserDepositService userDepositService;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;
    @Resource
    private VipOrderService vipOrderService;

    @Override
    public void saveInfo(UserWalletSaveBo userWalletSaveBo) {
        UserWallet userWallet = new UserWallet();
        BeanUtils.copyProperties(userWalletSaveBo, userWallet);

        iUserWalletService.save(userWallet);
    }

    @Override
    public IPage<UserWalletVo> pageInfo(UserWalletPageBo userWalletPageBo) {
        Page<UserWallet> initPage = PageUtil.initPage(userWalletPageBo.getPage(), userWalletPageBo.getLimit());

        QueryWrapper<UserWallet> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Objects.nonNull(userWalletPageBo.getUserId()), UserWallet::getUserId, userWalletPageBo.getUserId())
                .orderByDesc(UserWallet::getCreateTime);

        IPage<UserWallet> iPage = iUserWalletService.page(initPage, queryWrapper);
        List<UserWalletVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (UserWallet record : iPage.getRecords()) {
                UserWalletVo vo = new UserWalletVo();
                BeanUtils.copyProperties(record, vo);

                voList.add(vo);
            }
        }

        IPage<UserWalletVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public UserWalletVo info(UserWalletInfoBo userWalletInfoBo) {
        UserWallet userWallet = iUserWalletService.getById(userWalletInfoBo.getId());
        if (Objects.isNull(userWallet)) {
            throw new BizException("信息不存在");
        }

        UserWalletVo vo = new UserWalletVo();
        BeanUtils.copyProperties(userWallet, vo);
        return vo;
    }

    @Override
    public void updateInfo(UserWalletUpdateBo userWalletUpdateBo) {
        UserWallet userWallet = new UserWallet();
        BeanUtils.copyProperties(userWalletUpdateBo, userWallet);

        iUserWalletService.updateById(userWallet);
    }

    @Override
    public void delete(UserWalletInfoBo userWalletInfoBo) {
        iUserWalletService.removeById(userWalletInfoBo.getId());
    }

    @Override
    public UserWalletVo wallet() {
        Long userId = UserContextHolder.getAppUserId();
        ensureWalletExists(userId);
        UserWallet userWallet = iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("limit 1"));

        UserWalletVo vo = new UserWalletVo();
        BeanUtils.copyProperties(userWallet, vo);
        return vo;
    }

    @Override
    public void updateAmount(UserWalletUpdateAmountBo userWalletUpdateAmountBo) {
        if (Objects.equals(Operator.INCR.getValue(), userWalletUpdateAmountBo.getOperator())) {
            iUserWalletService.incr(userWalletUpdateAmountBo.getUserId(), userWalletUpdateAmountBo.getAmount());
            recordTransaction(userWalletUpdateAmountBo, 1, "管理员增加余额", String.format("管理员增加余额: %s", userWalletUpdateAmountBo.getAmount()));
            return;
        }

        if (Objects.equals(Operator.DECR.getValue(), userWalletUpdateAmountBo.getOperator())) {
            iUserWalletService.decr(userWalletUpdateAmountBo.getUserId(), userWalletUpdateAmountBo.getAmount());
            recordTransaction(userWalletUpdateAmountBo, 2, "管理员扣除余额", String.format("管理员扣除余额: %s", userWalletUpdateAmountBo.getAmount()));
            return;
        }

        log.error("不支持的操作: [{}]", userWalletUpdateAmountBo.getOperator());
        throw new BizException("不支持的操作");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payWithBalance(UserWalletPayBalanceBo bo) {
        Long userId = UserContextHolder.getAppUserId();
        ensureWalletExists(userId);
        UserWallet wallet = requireWallet(userId);

        BusinessType businessType = BusinessType.fromCode(bo.getBusinessType());
        BusinessParam param = new BusinessParam();
        param.setUserId(userId);
        param.setOrderNo(bo.getOrderNo());
        param.setBusinessType(bo.getBusinessType());
        businessService.process(param);

        BigDecimal payAmount = param.getAmount();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("订单金额异常");
        }

        BigDecimal walletAmount = wallet.getAmount() == null ? BigDecimal.ZERO : wallet.getAmount();
        if (walletAmount.compareTo(payAmount) < 0) {
            throw new BizException("余额不足");
        }

        int affected = iUserWalletService.decr(userId, payAmount);
        if (affected <= 0) {
            throw new BizException("余额不足");
        }

        String payNo = "BAL" + GenUtil.genOrderNo();
        saveBalancePayTransaction(userId, payAmount, bo.getOrderNo(), businessType.getCode(), payNo);
        completeBalancePayment(businessType, userId, bo.getOrderNo(), payAmount, payNo);
    }

    @Override
    public String applyRecharge(Long userId, UserWalletRechargeBo rechargeBo) {
        ensureWalletExists(userId);
        String payNo = RECHARGE_PAY_NO_PREFIX + GenUtil.genOrderNo();
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(rechargeBo.getAmount());
        transaction.setTransactionType(TX_TYPE_RECHARGE);
        transaction.setDirection(1);
        transaction.setStatus(TX_STATUS_PENDING);
        transaction.setTitle("余额充值");
        transaction.setDescription(payNo);
        transaction.setBizType(WalletBizType.RECHARGE.getCode());
        if (StringUtils.isNotBlank(rechargeBo.getRemark())) {
            transaction.setExtraData(String.format("{\"remark\":\"%s\"}", rechargeBo.getRemark().trim()));
        }
        iUserWalletTransactionService.save(transaction);
        return payNo;
    }

    @Override
    public BigDecimal prepareRechargePay(Long userId, String payNo, BigDecimal amount) {
        UserWalletTransaction pending = requirePendingRecharge(userId, payNo);
        if (amount != null && pending.getAmount().compareTo(amount) != 0) {
            throw new BizException("充值金额不一致");
        }
        return pending.getAmount();
    }

    @Override
    public UserWalletRechargePayStatusVo rechargePayStatus(Long userId, String payNo) {
        UserWalletTransaction transaction = iUserWalletTransactionService.getOne(new LambdaQueryWrapper<UserWalletTransaction>()
                .eq(UserWalletTransaction::getUserId, userId)
                .eq(UserWalletTransaction::getDescription, payNo)
                .eq(UserWalletTransaction::getTransactionType, TX_TYPE_RECHARGE)
                .last("LIMIT 1"));
        if (transaction == null) {
            throw new BizException("充值支付单不存在");
        }
        UserWalletRechargePayStatusVo vo = new UserWalletRechargePayStatusVo();
        vo.setPayNo(payNo);
        boolean paid = Objects.equals(transaction.getStatus(), TX_STATUS_COMPLETED);
        vo.setPaid(paid);
        vo.setStatus(paid ? "completed" : "pending");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRechargePaySuccess(Long userId, String payNo, BigDecimal amount) {
        UserWalletTransaction pending = iUserWalletTransactionService.getOne(new LambdaQueryWrapper<UserWalletTransaction>()
                .eq(UserWalletTransaction::getDescription, payNo)
                .eq(UserWalletTransaction::getStatus, TX_STATUS_PENDING)
                .eq(UserWalletTransaction::getTransactionType, TX_TYPE_RECHARGE)
                .last("LIMIT 1"));
        if (pending == null) {
            return;
        }
        Long targetUserId = userId != null ? userId : pending.getUserId();
        BigDecimal rechargeAmount = amount != null ? amount : pending.getAmount();
        ensureWalletExists(targetUserId);
        iUserWalletService.incr(targetUserId, rechargeAmount);
        pending.setStatus(TX_STATUS_COMPLETED);
        pending.setTitle("余额充值");
        pending.setDescription("余额充值成功");
        iUserWalletTransactionService.updateById(pending);
    }

    private UserWalletTransaction requirePendingRecharge(Long userId, String payNo) {
        UserWalletTransaction pending = iUserWalletTransactionService.getOne(new LambdaQueryWrapper<UserWalletTransaction>()
                .eq(UserWalletTransaction::getUserId, userId)
                .eq(UserWalletTransaction::getDescription, payNo)
                .eq(UserWalletTransaction::getStatus, TX_STATUS_PENDING)
                .eq(UserWalletTransaction::getTransactionType, TX_TYPE_RECHARGE)
                .last("LIMIT 1"));
        if (pending == null) {
            throw new BizException("充值支付单不存在或已处理");
        }
        return pending;
    }

    private void completeBalancePayment(BusinessType businessType, Long userId, String orderNo,
                                        BigDecimal payAmount, String payNo) {
        switch (businessType) {
            case RENT_ORDER:
                rentOrderService.handlePaySuccess(orderNo, PAY_CHANNEL_BALANCE, payNo, payAmount);
                return;
            case RENT_RENEW:
                rentRenewService.handleRenewPaySuccess(orderNo, PAY_CHANNEL_BALANCE, payNo, payAmount);
                return;
            case DEPOSIT_PAY:
                userDepositService.onDepositPaySuccess(userId, orderNo, payAmount);
                return;
            case MERCHANT_DEPOSIT_PAY:
                merchantSecurityDepositService.onPaySuccess(null, orderNo, payAmount);
                return;
            case WALLET_RECHARGE:
                onRechargePaySuccess(userId, orderNo, payAmount);
                return;
            case VIP:
                vipOrderService.handlePaySuccess(orderNo, PAY_CHANNEL_BALANCE, payNo, payAmount);
                return;
            default:
                throw new BizException("不支持的业务类型");
        }
    }

    private UserWallet requireWallet(Long userId) {
        UserWallet wallet = iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("limit 1"));
        if (wallet == null) {
            throw new BizException("钱包不存在");
        }
        return wallet;
    }

    private void ensureWalletExists(Long userId) {
        UserWallet wallet = iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("limit 1"));
        if (wallet != null) {
            return;
        }
        UserWallet newWallet = new UserWallet();
        newWallet.setUserId(userId);
        newWallet.setAmount(BigDecimal.ZERO);
        newWallet.setFrozenAmount(BigDecimal.ZERO);
        iUserWalletService.save(newWallet);
    }

    private void saveBalancePayTransaction(Long userId, BigDecimal amount, String orderNo, String bizType, String payNo) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setTransactionType(2);
        transaction.setDirection(2);
        transaction.setStatus(2);
        transaction.setTitle("余额支付");
        transaction.setDescription("余额支付");
        transaction.setBizType(WalletBizType.ORDER.getCode());
        transaction.setExtraData(String.format("{\"ref\":\"%s\",\"bizType\":\"%s\",\"payNo\":\"%s\"}",
                orderNo, bizType, payNo));
        try {
            iUserWalletTransactionService.save(transaction);
        } catch (Exception e) {
            log.warn("保存余额支付流水失败: userId={}, orderNo={}", userId, orderNo, e);
        }
    }

    /**
     * 记录钱包交易流水
     */
    private void recordTransaction(UserWalletUpdateAmountBo bo, int direction, String title, String description) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setUserId(bo.getUserId());
        transaction.setAmount(bo.getAmount());
        transaction.setTransactionType(5);
        transaction.setStatus(2);
        transaction.setDirection(direction);
        transaction.setTitle(title);
        transaction.setDescription(description);

        try {
            iUserWalletTransactionService.save(transaction);
        } catch (Exception e) {
            log.warn("保存交易记录失败，用户ID: {}, 金额: {}", bo.getUserId(), bo.getAmount(), e);
        }
    }

}

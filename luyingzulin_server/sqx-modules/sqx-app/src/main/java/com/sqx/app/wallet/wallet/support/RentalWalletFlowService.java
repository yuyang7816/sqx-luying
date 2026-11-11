package com.sqx.app.wallet.wallet.support;

import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.app.wallet.wallet.enums.WalletBizType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 租赁场景钱包流水写入
 */
@Slf4j
@Service
public class RentalWalletFlowService {

    @Resource
    private IUserWalletService iUserWalletService;
    @Resource
    private IUserWalletTransactionService iUserWalletTransactionService;

    @Transactional(rollbackFor = Exception.class)
    public void recordRentPay(Long userId, Long orderId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        saveTransaction(userId, orderId, amount, 2, WalletBizType.RENT_PAY.getCode(),
                "租赁支付", "订单号 " + orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordDepositFreeze(Long userId, Long orderId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        freezeWallet(userId, amount);
        saveTransaction(userId, orderId, amount, 2, WalletBizType.DEPOSIT_FREEZE.getCode(),
                "押金冻结", "订单号 " + orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordDepositRefund(Long userId, Long orderId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        unfreezeWallet(userId, amount);
        saveTransaction(userId, orderId, amount, 1, WalletBizType.DEPOSIT_REFUND.getCode(),
                "押金退还", "订单号 " + orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordWithdraw(Long userId, Long withdrawId, BigDecimal amount) {
        saveTransaction(userId, withdrawId, amount, 2, WalletBizType.WITHDRAW.getCode(),
                "余额提现", "提现申请");
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordWithdrawRejectRefund(Long userId, Long withdrawId, BigDecimal amount) {
        iUserWalletService.incr(userId, amount);
        saveTransaction(userId, withdrawId, amount, 1, WalletBizType.WITHDRAW_REJECT.getCode(),
                "提现驳回返还", "提现驳回，余额已返还");
    }

    private void freezeWallet(Long userId, BigDecimal amount) {
        ensureWallet(userId);
        iUserWalletService.incrFrozen(userId, amount);
    }

    private void unfreezeWallet(Long userId, BigDecimal amount) {
        ensureWallet(userId);
        iUserWalletService.decrFrozen(userId, amount);
    }

    private void ensureWallet(Long userId) {
        UserWallet wallet = iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
        if (wallet != null) {
            return;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setAmount(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        iUserWalletService.save(wallet);
    }

    private UserWallet requireWallet(Long userId) {
        ensureWallet(userId);
        return iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("LIMIT 1"));
    }

    private void saveTransaction(Long userId, Long bizId, BigDecimal amount, int direction,
                                 int bizType, String title, String description) {
        UserWalletTransaction tx = new UserWalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setTransactionType(direction == 1 ? 3 : 2);
        tx.setBizType(bizType);
        tx.setBizId(bizId);
        tx.setStatus(2);
        tx.setTitle(title);
        tx.setDescription(description);
        try {
            iUserWalletTransactionService.save(tx);
        } catch (Exception e) {
            log.warn("保存钱包流水失败 userId={} bizType={}: {}", userId, bizType, e.getMessage());
        }
    }
}

package com.sqx.app.wallet.wallet.db.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqx.app.wallet.wallet.db.mapper.UserWalletMapper;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * <p>
 * 用户钱包主表 服务实现类
 * </p>
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-13 15:09:58
 */
@Service
public class IUserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements IUserWalletService {

    @Resource
    private UserWalletMapper userWalletMapper;

    @Override
    public void incr(Long userId, BigDecimal amount) {
        userWalletMapper.incr(userId, amount);
    }

    @Override
    public int decr(Long userId, BigDecimal amount) {
        return userWalletMapper.decr(userId, amount);
    }

    @Override
    public void incrFrozen(Long userId, BigDecimal amount) {
        userWalletMapper.incrFrozen(userId, amount);
    }

    @Override
    public void decrFrozen(Long userId, BigDecimal amount) {
        userWalletMapper.decrFrozen(userId, amount);
    }
}

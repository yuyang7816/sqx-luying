package com.sqx.app.wallet.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionPageBo;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.transaction.domain.vo.UserWalletTransactionVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWalletTransactionServiceImplTest {

    @Mock
    private IUserWalletTransactionService iUserWalletTransactionService;
    @Mock
    private IUserService iUserService;

    @InjectMocks
    private UserWalletTransactionServiceImpl userWalletTransactionService;

    @Test
    void pageInfoShouldReturnRecordsForUser() {
        UserWalletTransaction tx = new UserWalletTransaction();
        tx.setId(1L);
        tx.setUserId(100L);
        tx.setAmount(new BigDecimal("10.00"));
        tx.setDirection(1);
        tx.setTitle("充值");

        Page<UserWalletTransaction> page = new Page<>(1, 20);
        page.setRecords(Collections.singletonList(tx));
        page.setTotal(1);

        UserEntity user = new UserEntity();
        user.setUserId(100L);
        user.setPhone("13800000000");
        user.setUserName("tester");

        when(iUserWalletTransactionService.page(any(), any())).thenReturn(page);
        when(iUserService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(user));

        UserWalletTransactionPageBo pageBo = new UserWalletTransactionPageBo();
        pageBo.setPage(1L);
        pageBo.setLimit(20L);
        pageBo.setUserId(100L);

        IPage<UserWalletTransactionVo> result = userWalletTransactionService.pageInfo(pageBo);

        assertEquals(1, result.getRecords().size());
        assertEquals("13800000000", result.getRecords().get(0).getPhone());
        assertEquals("充值", result.getRecords().get(0).getTitle());
    }

    @Test
    void pageInfoShouldReturnEmptyWhenNoRecords() {
        Page<UserWalletTransaction> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(iUserWalletTransactionService.page(any(), any())).thenReturn(page);

        UserWalletTransactionPageBo pageBo = new UserWalletTransactionPageBo();
        pageBo.setPage(1L);
        pageBo.setLimit(20L);
        pageBo.setUserId(100L);

        IPage<UserWalletTransactionVo> result = userWalletTransactionService.pageInfo(pageBo);

        assertEquals(0, result.getRecords().size());
    }
}

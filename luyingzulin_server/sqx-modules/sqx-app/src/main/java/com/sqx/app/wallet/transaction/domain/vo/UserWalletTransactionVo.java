package com.sqx.app.wallet.transaction.domain.vo;

import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-13 15:12:03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserWalletTransactionVo extends UserWalletTransaction {

    /** 用户手机号（管理端明文） */
    private String phone;

    /** 用户名 */
    private String userName;
}

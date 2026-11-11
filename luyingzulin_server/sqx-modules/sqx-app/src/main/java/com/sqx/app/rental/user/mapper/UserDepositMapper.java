package com.sqx.app.rental.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.rental.user.domain.entity.UserDeposit;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserDepositMapper extends BaseMapper<UserDeposit> {

    void incrTotal(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    void decrTotal(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    void incrFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    void decrFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int freezeAvailable(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int unfreezeAvailable(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int refundFrozen(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}

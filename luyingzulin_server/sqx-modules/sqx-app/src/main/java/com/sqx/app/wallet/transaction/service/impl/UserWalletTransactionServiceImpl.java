package com.sqx.app.wallet.transaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionInfoBo;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionPageBo;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionSaveBo;
import com.sqx.app.wallet.transaction.domain.bo.UserWalletTransactionUpdateBo;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.transaction.domain.vo.UserWalletTransactionVo;
import com.sqx.app.wallet.transaction.service.UserWalletTransactionService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户钱包交易记录表 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-13 15:12:03
 */
@Service
public class UserWalletTransactionServiceImpl implements UserWalletTransactionService {

    @Resource
    private IUserWalletTransactionService iUserWalletTransactionService;
    @Resource
    private IUserService iUserService;

    @Override
    public void saveInfo(UserWalletTransactionSaveBo userWalletTransactionSaveBo) {
        UserWalletTransaction userWalletTransaction = new UserWalletTransaction();
        BeanUtils.copyProperties(userWalletTransactionSaveBo, userWalletTransaction);

        iUserWalletTransactionService.save(userWalletTransaction);
    }

    @Override
    public IPage<UserWalletTransactionVo> pageInfo(UserWalletTransactionPageBo userWalletTransactionPageBo) {
        Page<UserWalletTransaction> initPage = PageUtil.initPage(userWalletTransactionPageBo.getPage(), userWalletTransactionPageBo.getLimit());

        QueryWrapper<UserWalletTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Objects.nonNull(userWalletTransactionPageBo.getUserId()), UserWalletTransaction::getUserId, userWalletTransactionPageBo.getUserId())
                .orderByDesc(UserWalletTransaction::getCreateTime);

        IPage<UserWalletTransaction> iPage = iUserWalletTransactionService.page(initPage, queryWrapper);
        List<UserWalletTransactionVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            List<Long> userIds = iPage.getRecords().stream()
                    .map(UserWalletTransaction::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, UserEntity> userMap = loadUserBriefMap(userIds);

            for (UserWalletTransaction record : iPage.getRecords()) {
                UserWalletTransactionVo vo = new UserWalletTransactionVo();
                BeanUtils.copyProperties(record, vo);
                UserEntity user = userMap.get(record.getUserId());
                if (user != null) {
                    vo.setPhone(user.getPhone());
                    vo.setUserName(user.getUserName());
                }
                voList.add(vo);
            }
        }

        IPage<UserWalletTransactionVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public UserWalletTransactionVo info(UserWalletTransactionInfoBo userWalletTransactionInfoBo) {
        UserWalletTransaction userWalletTransaction = iUserWalletTransactionService.getById(userWalletTransactionInfoBo.getId());
        if (Objects.isNull(userWalletTransaction)) {
            throw new BizException("信息不存在");
        }

        UserWalletTransactionVo vo = new UserWalletTransactionVo();
        BeanUtils.copyProperties(userWalletTransaction, vo);
        UserEntity user = loadUserBrief(userWalletTransaction.getUserId());
        if (user != null) {
            vo.setPhone(user.getPhone());
            vo.setUserName(user.getUserName());
        }
        return vo;
    }

    private Map<Long, UserEntity> loadUserBriefMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return iUserService.list(new LambdaQueryWrapper<UserEntity>()
                        .select(UserEntity::getUserId, UserEntity::getPhone, UserEntity::getUserName)
                        .in(UserEntity::getUserId, userIds))
                .stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(UserEntity::getUserId, user -> user, (a, b) -> a));
    }

    private UserEntity loadUserBrief(Long userId) {
        if (userId == null) {
            return null;
        }
        return iUserService.getOne(new LambdaQueryWrapper<UserEntity>()
                .select(UserEntity::getUserId, UserEntity::getPhone, UserEntity::getUserName)
                .eq(UserEntity::getUserId, userId)
                .last("LIMIT 1"));
    }

    @Override
    public void updateInfo(UserWalletTransactionUpdateBo userWalletTransactionUpdateBo) {
        UserWalletTransaction userWalletTransaction = new UserWalletTransaction();
        BeanUtils.copyProperties(userWalletTransactionUpdateBo, userWalletTransaction);

        iUserWalletTransactionService.updateById(userWalletTransaction);
    }

    @Override
    public void delete(UserWalletTransactionInfoBo userWalletTransactionInfoBo) {
        iUserWalletTransactionService.removeById(userWalletTransactionInfoBo.getId());
    }
}

package com.sqx.app.referral.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.support.UserAvatarSupport;
import com.sqx.app.referral.commission.db.service.IReferralCommissionService;
import com.sqx.app.referral.commission.domain.entity.ReferralCommission;
import com.sqx.app.referral.commission.domain.vo.ReferralListVo;
import com.sqx.app.referral.user.db.service.IReferralUserCommissionService;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionInfoBo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionPageBo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionUpdateBo;
import com.sqx.app.referral.user.domain.entity.ReferralUserCommission;
import com.sqx.app.referral.user.domain.vo.AdminReferralUserCommissionListVo;
import com.sqx.app.referral.user.domain.vo.ReferralDataVo;
import com.sqx.app.referral.user.domain.vo.ReferralLawyerVo;
import com.sqx.app.referral.user.domain.vo.ReferralTeamMemberVo;
import com.sqx.app.referral.user.domain.vo.ReferralTeamOverviewVo;
import com.sqx.app.referral.user.domain.vo.ReferralUserCommissionVo;
import com.sqx.app.referral.user.service.ReferralUserCommissionService;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.utils.DateUtil;
import com.sqx.app.utils.model.StartAndEndTime;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.shiro.utils.UserContextHolder;
import com.sqx.system.db.service.ISysConfigService;
import com.sqx.common.mybatis.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户佣金 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-04 14:44:53
 */
@Slf4j
@Service
public class ReferralUserCommissionServiceImpl implements ReferralUserCommissionService {

    @Resource
    private IReferralUserCommissionService iReferralUserCommissionService;
    @Resource
    private ISysConfigService iSysConfigService;
    @Resource
    private IUserService iUserService;
    @Resource
    private IReferralCommissionService iReferralCommissionService;
    @Resource
    private RentOrderMapper rentOrderMapper;

    private static final String COMMISSION_RATE_KEY = "biz_referral_commission_rate";
    private static final String COMMISSION_RATE_LEVEL1_KEY = "biz_referral_commission_rate_level1";
    private static final String COMMISSION_RATE_LEVEL2_KEY = "biz_referral_commission_rate_level2";
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_COMMISSION_RATE_LEVEL2 = new BigDecimal("0.02");

    @Override
    public ReferralUserCommissionVo info(Long userId) {
        if (userId == null) {
            throw new BizException("用户不存在");
        }
        ReferralUserCommission referralUserCommission = ensureUserCommission(userId);

        ReferralUserCommissionVo vo = new ReferralUserCommissionVo();
        BeanUtils.copyProperties(referralUserCommission, vo);
        return vo;
    }

    private ReferralUserCommission ensureUserCommission(Long userId) {
        ReferralUserCommission referralUserCommission = iReferralUserCommissionService.getByUserId(userId);
        if (referralUserCommission != null) {
            return referralUserCommission;
        }
        UserEntity user = iUserService.getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        referralUserCommission = new ReferralUserCommission();
        referralUserCommission.setUserId(userId);
        referralUserCommission.setCommission(BigDecimal.ZERO);
        iReferralUserCommissionService.save(referralUserCommission);
        return referralUserCommission;
    }

    @Override
    public IPage<AdminReferralUserCommissionListVo> adminPage(ReferralUserCommissionPageBo pageBo) {
        Page<ReferralUserCommission> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<ReferralUserCommission> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getPhone()) || StringUtils.isNotBlank(pageBo.getUserName())) {
            LambdaQueryWrapper<UserEntity> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(StringUtils.isNotBlank(pageBo.getPhone()), UserEntity::getPhone, pageBo.getPhone())
                    .like(StringUtils.isNotBlank(pageBo.getUserName()), UserEntity::getUserName, pageBo.getUserName());
            List<UserEntity> users = iUserService.list(userWrapper);
            if (CollectionUtils.isEmpty(users)) {
                return PageUtil.copyPage(page, Collections.emptyList());
            }
            List<Long> userIds = users.stream().map(UserEntity::getUserId).collect(Collectors.toList());
            wrapper.in(ReferralUserCommission::getUserId, userIds);
        }
        wrapper.orderByDesc(ReferralUserCommission::getCommission);
        IPage<ReferralUserCommission> entityPage = iReferralUserCommissionService.page(page, wrapper);
        if (CollectionUtils.isEmpty(entityPage.getRecords())) {
            return PageUtil.copyPage(entityPage, Collections.emptyList());
        }
        List<Long> userIds = entityPage.getRecords().stream()
                .map(ReferralUserCommission::getUserId).collect(Collectors.toList());
        Map<Long, UserEntity> userMap = iUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
        List<AdminReferralUserCommissionListVo> records = entityPage.getRecords().stream().map(item -> {
            AdminReferralUserCommissionListVo vo = new AdminReferralUserCommissionListVo();
            vo.setId(item.getId());
            vo.setUserId(item.getUserId());
            vo.setCommission(item.getCommission());
            UserEntity user = userMap.get(item.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setPhone(user.getPhone());
                vo.setPhoneMasked(RentalUserSupport.maskPhone(user.getPhone()));
                long inviteCount = iUserService.count(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getSuperiorInvitationCode, user.getInvitationCode()));
                vo.setInviteCount(inviteCount);
            }
            return vo;
        }).collect(Collectors.toList());
        return PageUtil.copyPage(entityPage, records);
    }

    @Override
    public void updateInfo(ReferralUserCommissionUpdateBo referralUserCommissionUpdateBo) {
        ReferralUserCommission referralUserCommission = new ReferralUserCommission();
        BeanUtils.copyProperties(referralUserCommissionUpdateBo, referralUserCommission);

        iReferralUserCommissionService.updateById(referralUserCommission);
    }

    @Override
    public void delete(ReferralUserCommissionInfoBo referralUserCommissionInfoBo) {
        iReferralUserCommissionService.removeById(referralUserCommissionInfoBo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserCommission(Long orderId) {
        if (orderId == null) {
            return;
        }
        RentOrder order = rentOrderMapper.selectById(orderId);
        if (order == null) {
            log.info("租赁订单不存在, 不计算佣金 orderId={}", orderId);
            return;
        }

        long existing = iReferralCommissionService.count(new LambdaQueryWrapper<ReferralCommission>()
                .eq(ReferralCommission::getOrderId, orderId)
                .eq(ReferralCommission::getType, 1));
        if (existing >= 2) {
            log.info("订单 {} 一二级佣金均已结算，跳过", orderId);
            return;
        }

        UserEntity buyer = iUserService.getById(order.getUserId());
        if (buyer == null || StringUtils.isBlank(buyer.getSuperiorInvitationCode())) {
            log.info("用户无上级邀请码，不计算佣金 userId={}", order.getUserId());
            return;
        }

        UserEntity inviter = iUserService.getOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getInvitationCode, buyer.getSuperiorInvitationCode())
                .last("LIMIT 1"));
        if (inviter == null) {
            log.info("邀请人不存在，不计算佣金 code={}", buyer.getSuperiorInvitationCode());
            return;
        }
        if (!Objects.equals(1, inviter.getStatus())) {
            log.info("邀请用户被禁用，取消计算佣金 inviterId={}", inviter.getUserId());
            return;
        }

        BigDecimal payBase = calcRentPayBase(order);
        if (payBase.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("订单 {} 可计佣金额为零，跳过", order.getOrderNo());
            return;
        }

        grantLevelCommission(order, inviter, payBase, 1);

        if (StringUtils.isBlank(inviter.getSuperiorInvitationCode())) {
            return;
        }
        UserEntity level2Inviter = iUserService.getOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getInvitationCode, inviter.getSuperiorInvitationCode())
                .last("LIMIT 1"));
        if (level2Inviter == null) {
            log.info("二级邀请人不存在，跳过 code={}", inviter.getSuperiorInvitationCode());
            return;
        }
        if (!Objects.equals(1, level2Inviter.getStatus())) {
            log.info("二级邀请用户被禁用，跳过 inviterId={}", level2Inviter.getUserId());
            return;
        }
        grantLevelCommission(order, level2Inviter, payBase, 2);
    }

    private void grantLevelCommission(RentOrder order, UserEntity inviter, BigDecimal payBase, int level) {
        long existing = iReferralCommissionService.count(new LambdaQueryWrapper<ReferralCommission>()
                .eq(ReferralCommission::getOrderId, order.getId())
                .eq(ReferralCommission::getUserId, inviter.getUserId())
                .eq(ReferralCommission::getType, 1));
        if (existing > 0) {
            log.info("订单 {} 用户 {} 佣金已结算，跳过", order.getOrderNo(), inviter.getUserId());
            return;
        }
        BigDecimal commissionRate = resolveCommissionRate(level);
        BigDecimal commission = payBase.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        if (commission.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        iReferralUserCommissionService.incr(inviter.getUserId(), commission);

        ReferralCommission referralCommission = new ReferralCommission();
        referralCommission.setUserId(inviter.getUserId());
        referralCommission.setOrderId(order.getId());
        referralCommission.setRate(commissionRate);
        referralCommission.setAmount(commission);
        referralCommission.setType(1);
        referralCommission.setDirection(1);
        referralCommission.setTitle(level == 1 ? "租赁订单一级分佣" : "租赁订单二级分佣");
        referralCommission.setDescription(String.format("%s下级订单 %s 完成，获得分佣 ¥%s",
                level == 1 ? "一级" : "二级", order.getOrderNo(), commission.toPlainString()));
        iReferralCommissionService.save(referralCommission);
    }

    private BigDecimal resolveCommissionRate(int level) {
        if (level == 2) {
            return resolveConfigRate(COMMISSION_RATE_LEVEL2_KEY, DEFAULT_COMMISSION_RATE_LEVEL2);
        }
        BigDecimal level1 = resolveConfigRate(COMMISSION_RATE_LEVEL1_KEY, null);
        if (level1 != null) {
            return level1;
        }
        return resolveConfigRate(COMMISSION_RATE_KEY, DEFAULT_COMMISSION_RATE);
    }

    private BigDecimal resolveConfigRate(String key, BigDecimal defaultValue) {
        try {
            String rateStr = iSysConfigService.getConfigValue(key);
            if (StringUtils.isNotBlank(rateStr)) {
                return new BigDecimal(rateStr.trim());
            }
        } catch (Exception e) {
            log.warn("读取分佣比例失败 key={}: {}", key, e.getMessage());
        }
        return defaultValue;
    }

    private static BigDecimal calcRentPayBase(RentOrder order) {
        BigDecimal rent = order.getRentAmount() == null ? BigDecimal.ZERO : order.getRentAmount();
        BigDecimal delivery = order.getDeliveryAmount() == null ? BigDecimal.ZERO : order.getDeliveryAmount();
        BigDecimal coupon = order.getCouponAmount() == null ? BigDecimal.ZERO : order.getCouponAmount();
        return rent.add(delivery).subtract(coupon).max(BigDecimal.ZERO);
    }

    @Override
    public ReferralDataVo referralData(Long userId) {
        UserEntity user = iUserService.getById(userId);
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }

        // 邀请人数
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getSuperiorInvitationCode, user.getInvitationCode());
        long count = iUserService.count(queryWrapper);

        // 累计佣金（订单分佣明细汇总）
        BigDecimal commission = iReferralCommissionService.sumUserCommission(userId);
        if (commission == null) {
            commission = BigDecimal.ZERO;
        }

        ReferralDataVo vo = new ReferralDataVo();
        vo.setNum(count);
        vo.setCommission(commission);
        return vo;
    }

    @Override
    public IPage<AdminUserListVo> adminInviteList(Long userId, Integer page, Integer limit) {
        UserEntity user = iUserService.getById(userId);
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }
        Page<UserEntity> invitePage = PageUtil.initPage(
                page == null ? 1L : page.longValue(),
                limit == null ? 10L : limit.longValue());
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(UserEntity::getSuperiorInvitationCode, user.getInvitationCode())
                .orderByDesc(UserEntity::getCreateTime);
        IPage<UserEntity> entityPage = iUserService.page(invitePage, queryWrapper);
        List<AdminUserListVo> records = entityPage.getRecords().stream()
                .map(RentalUserSupport::toAdminUserListVo)
                .collect(Collectors.toList());
        return PageUtil.copyPage(entityPage, records);
    }

    @Override
    public IPage<UserEntity> referralUserList(Long userId) {
        Page<UserEntity> page = new Page<>(1, 10);

        UserEntity user = iUserService.getById(userId);
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getSuperiorInvitationCode, user.getInvitationCode());
        IPage<UserEntity> userEntityPage = iUserService.page(page, queryWrapper);
        if (userEntityPage.getRecords() != null) {
            userEntityPage.getRecords().forEach(item ->
                    item.setAvatar(UserAvatarSupport.resolveAvatar(item.getAvatar())));
        }

        return userEntityPage;
    }

    @Override
    public ReferralTeamOverviewVo referralTeamOverview(Long userId) {
        UserEntity user = requireUser(userId);
        String invitationCode = user.getInvitationCode();
        long level1Count = countDirectInvites(invitationCode);
        long level2Count = countLevel2Invites(invitationCode);
        BigDecimal level1Commission = sumCommissionByLevel(userId, 1);
        BigDecimal level2Commission = sumCommissionByLevel(userId, 2);
        BigDecimal totalCommission = level1Commission.add(level2Commission);

        ReferralTeamOverviewVo vo = new ReferralTeamOverviewVo();
        vo.setLevel1Count(level1Count);
        vo.setLevel2Count(level2Count);
        vo.setTotalCommission(totalCommission);
        vo.setLevel1Commission(level1Commission);
        vo.setLevel2Commission(level2Commission);
        vo.setLevel1RatePercent(toPercent(resolveCommissionRate(1)));
        vo.setLevel2RatePercent(toPercent(resolveCommissionRate(2)));
        return vo;
    }

    @Override
    public IPage<ReferralTeamMemberVo> referralTeamUserList(Long userId, Integer level, Integer page, Integer limit) {
        UserEntity user = requireUser(userId);
        int safeLevel = Objects.equals(level, 2) ? 2 : 1;
        Page<UserEntity> entityPage = PageUtil.initPage(
                page == null ? 1L : page.longValue(),
                limit == null ? 20L : limit.longValue());

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (safeLevel == 1) {
            wrapper.eq(UserEntity::getSuperiorInvitationCode, user.getInvitationCode());
        } else {
            List<String> level1Codes = listLevel1InvitationCodes(user.getInvitationCode());
            if (CollectionUtils.isEmpty(level1Codes)) {
                return PageUtil.copyPage(entityPage, Collections.emptyList());
            }
            wrapper.in(UserEntity::getSuperiorInvitationCode, level1Codes);
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);
        IPage<UserEntity> pageResult = iUserService.page(entityPage, wrapper);
        List<ReferralTeamMemberVo> records = pageResult.getRecords().stream()
                .map(this::toTeamMemberVo)
                .collect(Collectors.toList());
        return PageUtil.copyPage(pageResult, records);
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = iUserService.getById(userId);
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private long countDirectInvites(String invitationCode) {
        if (StringUtils.isBlank(invitationCode)) {
            return 0L;
        }
        return iUserService.count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getSuperiorInvitationCode, invitationCode));
    }

    private List<String> listLevel1InvitationCodes(String invitationCode) {
        if (StringUtils.isBlank(invitationCode)) {
            return Collections.emptyList();
        }
        return iUserService.list(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getSuperiorInvitationCode, invitationCode)
                        .select(UserEntity::getInvitationCode))
                .stream()
                .map(UserEntity::getInvitationCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private long countLevel2Invites(String invitationCode) {
        List<String> level1Codes = listLevel1InvitationCodes(invitationCode);
        if (CollectionUtils.isEmpty(level1Codes)) {
            return 0L;
        }
        return iUserService.count(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getSuperiorInvitationCode, level1Codes));
    }

    private BigDecimal sumCommissionByLevel(Long userId, int level) {
        String keyword = level == 2 ? "二级" : "一级";
        List<ReferralCommission> records = iReferralCommissionService.list(new LambdaQueryWrapper<ReferralCommission>()
                .eq(ReferralCommission::getUserId, userId)
                .like(ReferralCommission::getTitle, keyword));
        if (CollectionUtils.isEmpty(records)) {
            return BigDecimal.ZERO;
        }
        return records.stream()
                .map(ReferralCommission::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal toPercent(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private ReferralTeamMemberVo toTeamMemberVo(UserEntity entity) {
        ReferralTeamMemberVo vo = new ReferralTeamMemberVo();
        vo.setUserId(entity.getUserId());
        vo.setNickname(StringUtils.isNotBlank(entity.getUserName()) ? entity.getUserName() : RentalUserSupport.maskPhone(entity.getPhone()));
        vo.setAvatar(UserAvatarSupport.resolveAvatar(entity.getAvatar()));
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    public IPage<ReferralListVo> referralList(Long userId) {
        Page<ReferralCommission> page = new Page<>(1, 10);

        IPage<ReferralListVo> voIPage = iReferralCommissionService.referralList(page, userId);

        return voIPage;
    }

    @Override
    public ReferralLawyerVo lawyer() {
        ReferralCommission referralCommission = iReferralCommissionService.getByUserId(UserContextHolder.getAppUserId());
        if (Objects.isNull(referralCommission)) {
            log.error("用户[{}]资产不存在", UserContextHolder.getAppUserId());
            throw new BizException("用户资产不存在");
        }

        // 昨日收益
        StartAndEndTime startAndEndTime = DateUtil.getStartAndEndTime(LocalDate.now().plusDays(1));
        BigDecimal yesterdayEarn = iReferralCommissionService.sumAmount(UserContextHolder.getAppUserId(), startAndEndTime.getStartTime(), startAndEndTime.getEndTime());

        ReferralLawyerVo vo = new ReferralLawyerVo();
        vo.setAmount(referralCommission.getAmount());
        vo.setYesterdayEarn(yesterdayEarn);
        return vo;
    }
}

package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.order.domain.bo.RentInspectionAuditBo;
import com.sqx.app.rental.order.domain.bo.RentInspectionPageBo;
import com.sqx.app.rental.order.domain.bo.RentInspectionSubmitBo;
import com.sqx.app.rental.order.domain.entity.RentInspection;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.vo.RentInspectionDetailVo;
import com.sqx.app.rental.order.domain.vo.RentInspectionListVo;
import com.sqx.app.rental.order.mapper.RentInspectionMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.service.RentInspectionService;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.order.support.RentOrderSupport;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 验收服务实现
 */
@Service
public class RentInspectionServiceImpl implements RentInspectionService {

    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentInspectionMapper rentInspectionMapper;
    @Resource
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private RentStockService rentStockService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserDepositService userDepositService;
    @Resource
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private com.sqx.app.rental.order.support.RentOrderNotificationSupport rentOrderNotificationSupport;
    @Resource
    private com.sqx.app.referral.user.service.ReferralUserCommissionService referralUserCommissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantSubmit(Long operatorId, RentInspectionSubmitBo submitBo) {
        merchantContextSupport.requireMerchantContextForOperation(operatorId);
        RentOrder order = RentOrderSupport.requireOrder(rentOrderMapper, submitBo.getOrderId());
        RentOrderStateMachine.assertCanInspect(order.getStatus());

        Long existing = rentInspectionMapper.selectCount(new LambdaQueryWrapper<RentInspection>()
                .eq(RentInspection::getOrderId, order.getId()));
        if (existing != null && existing > 0) {
            throw new BizException("该订单已验收");
        }

        String deviceCondition = StringUtils.defaultIfBlank(submitBo.getDeviceCondition(), "good");
        BigDecimal depositAmount = order.getDepositAmount() == null
                ? BigDecimal.ZERO : order.getDepositAmount();
        BigDecimal deductAmount = submitBo.getDeductAmount() == null
                ? BigDecimal.ZERO : submitBo.getDeductAmount().max(BigDecimal.ZERO);
        if (deductAmount.compareTo(depositAmount) > 0) {
            throw new BizException("扣款金额不能超过订单押金 ¥" + depositAmount);
        }
        if (StringUtils.isBlank(submitBo.getRemark())) {
            throw new BizException("请填写验收说明");
        }
        if (CollectionUtils.isEmpty(submitBo.getImageUrls())) {
            throw new BizException("请上传验收图片");
        }
        BigDecimal refundAmount = depositAmount.subtract(deductAmount).max(BigDecimal.ZERO);

        String targetStatus = OrderStatus.COMPLETED.getCode();
        RentOrderStateMachine.assertTransition(order.getStatus(), targetStatus);

        LocalDateTime now = LocalDateTime.now();
        int updated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, order.getId())
                .in(RentOrder::getStatus,
                        OrderStatus.RETURNING.getCode(),
                        OrderStatus.INSPECTING.getCode())
                .set(RentOrder::getStatus, targetStatus)
                .set(RentOrder::getCompleteTime, now));
        if (updated == 0) {
            throw new BizException("验收提交失败，订单状态已变更");
        }

        rentStockService.finishOccupy(order.getId());

        rentReturnApplyMapper.update(null, new LambdaUpdateWrapper<RentReturnApply>()
                .eq(RentReturnApply::getOrderId, order.getId())
                .eq(RentReturnApply::getStatus, "pending")
                .set(RentReturnApply::getStatus, "completed"));

        RentInspection inspection = new RentInspection();
        inspection.setOrderId(order.getId());
        inspection.setOrderNo(order.getOrderNo());
        inspection.setMerchantId(order.getMerchantId());
        inspection.setInspectorType("merchant");
        inspection.setInspectorId(operatorId);
        inspection.setDeviceCondition(deviceCondition);
        inspection.setDeductAmount(deductAmount);
        inspection.setRefundAmount(refundAmount);
        inspection.setImageUrls(joinImages(submitBo.getImageUrls()));
        inspection.setRemark(submitBo.getRemark());
        inspection.setAuditStatus("approved");
        inspection.setAuditTime(now);
        rentInspectionMapper.insert(inspection);

        RentOrderSupport.writeOrderLog(rentOrderLogMapper, order.getId(), order.getOrderNo(),
                order.getStatus(), targetStatus, "merchant", operatorId,
                "商户验收完成，退还押金 " + refundAmount + " 元");

        userDepositService.refundAfterInspection(order.getUserId(), order.getOrderNo(),
                refundAmount, deductAmount);

        merchantOrderSyncSupport.markCompleted(order.getId(), now);

        RentOrder completed = rentOrderMapper.selectById(order.getId());
        if (completed != null) {
            rentOrderNotificationSupport.onInspectionComplete(completed, deductAmount, refundAmount);
        }
        try {
            referralUserCommissionService.saveUserCommission(order.getId());
        } catch (Exception e) {
            // 分佣失败不影响验收主流程
        }
    }

    @Override
    public IPage<RentInspectionListVo> adminPage(RentInspectionPageBo pageBo) {
        Page<RentInspection> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<RentInspection> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.and(w -> w.eq(RentInspection::getId, id).or().eq(RentInspection::getOrderId, id));
            }
        }
        if (StringUtils.isNotBlank(pageBo.getAuditStatus())) {
            wrapper.eq(RentInspection::getAuditStatus, pageBo.getAuditStatus());
        }
        if (StringUtils.isNotBlank(pageBo.getDeviceCondition())) {
            wrapper.eq(RentInspection::getDeviceCondition, pageBo.getDeviceCondition());
        }
        wrapper.orderByDesc(RentInspection::getCreateTime);

        IPage<RentInspection> inspectionPage = rentInspectionMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(inspectionPage.getRecords())) {
            return PageUtil.copyPage(inspectionPage, Collections.emptyList());
        }

        List<Long> orderIds = inspectionPage.getRecords().stream()
                .map(RentInspection::getOrderId).distinct().collect(Collectors.toList());
        Map<Long, RentOrder> orderMap = rentOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(RentOrder::getId, o -> o, (a, b) -> a));
        List<Long> userIds = orderMap.values().stream().map(RentOrder::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, UserEntity> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));

        List<RentInspectionListVo> records = inspectionPage.getRecords().stream()
                .map(ins -> toListVo(ins, orderMap.get(ins.getOrderId()), userMap))
                .collect(Collectors.toList());
        return PageUtil.copyPage(inspectionPage, records);
    }

    @Override
    public RentInspectionDetailVo adminDetail(Long id) {
        RentInspection inspection = rentInspectionMapper.selectById(id);
        if (inspection == null) {
            throw new BizException("验收记录不存在");
        }
        RentOrder order = rentOrderMapper.selectById(inspection.getOrderId());
        UserEntity user = order == null || order.getUserId() == null ? null
                : userMapper.selectById(order.getUserId());
        return toDetailVo(inspection, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAudit(RentInspectionAuditBo auditBo) {
        RentInspection inspection = rentInspectionMapper.selectById(auditBo.getId());
        if (inspection == null) {
            throw new BizException("验收记录不存在");
        }
        if (!"pending".equals(inspection.getAuditStatus())) {
            throw new BizException("该验收记录已复核");
        }
        if (!"approved".equals(auditBo.getAuditStatus())
                && !"rejected".equals(auditBo.getAuditStatus())) {
            throw new BizException("复核结果无效");
        }

        rentInspectionMapper.update(null, new LambdaUpdateWrapper<RentInspection>()
                .eq(RentInspection::getId, auditBo.getId())
                .eq(RentInspection::getAuditStatus, "pending")
                .set(RentInspection::getAuditStatus, auditBo.getAuditStatus())
                .set(RentInspection::getAuditRemark, auditBo.getAuditRemark())
                .set(RentInspection::getAuditTime, LocalDateTime.now()));
    }

    private RentInspectionListVo toListVo(RentInspection inspection, RentOrder order,
                                          Map<Long, UserEntity> userMap) {
        RentInspectionListVo vo = new RentInspectionListVo();
        vo.setId(inspection.getId());
        vo.setOrderId(inspection.getOrderId());
        vo.setOrderNo(inspection.getOrderNo());
        if (order != null && order.getUserId() != null) {
            UserEntity user = userMap.get(order.getUserId());
            vo.setUserNickName(user == null ? null : user.getUserName());
            vo.setUserPhone(user == null ? null : user.getPhone());
        }
        vo.setDeviceCondition(inspection.getDeviceCondition());
        vo.setDeductAmount(inspection.getDeductAmount());
        vo.setRefundAmount(inspection.getRefundAmount());
        vo.setAuditStatus(inspection.getAuditStatus());
        vo.setCreateTime(inspection.getCreateTime());
        return vo;
    }

    static RentInspectionDetailVo toDetailVo(RentInspection inspection, UserEntity user) {
        RentInspectionDetailVo vo = new RentInspectionDetailVo();
        vo.setId(inspection.getId());
        vo.setOrderId(inspection.getOrderId());
        vo.setOrderNo(inspection.getOrderNo());
        vo.setUserNickName(user == null ? null : user.getUserName());
        vo.setUserPhone(user == null ? null : user.getPhone());
        vo.setDeviceCondition(inspection.getDeviceCondition());
        vo.setDeductAmount(inspection.getDeductAmount());
        vo.setRefundAmount(inspection.getRefundAmount());
        vo.setImageUrls(splitImages(inspection.getImageUrls()));
        vo.setRemark(inspection.getRemark());
        vo.setAuditStatus(inspection.getAuditStatus());
        vo.setAuditRemark(inspection.getAuditRemark());
        vo.setAuditTime(inspection.getAuditTime());
        vo.setCreateTime(inspection.getCreateTime());
        return vo;
    }

    private static String joinImages(List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return null;
        }
        return String.join(",", imageUrls);
    }

    public static List<String> splitImages(String imageUrls) {
        if (StringUtils.isBlank(imageUrls)) {
            return Collections.emptyList();
        }
        return Arrays.stream(imageUrls.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}

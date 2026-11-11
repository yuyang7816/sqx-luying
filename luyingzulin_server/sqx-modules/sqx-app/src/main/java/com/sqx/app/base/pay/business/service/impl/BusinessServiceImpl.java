package com.sqx.app.base.pay.business.service.impl;

import com.sqx.app.base.pay.business.service.BusinessService;
import com.sqx.app.base.pay.domain.BusinessParam;
import com.sqx.app.base.pay.enums.BusinessType;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.app.rental.order.service.RentRenewService;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.app.wallet.wallet.service.UserWalletService;
import com.sqx.common.core.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 支付业务处理
 */
@Slf4j
@Service
public class BusinessServiceImpl implements BusinessService {

    @Resource
    private RentOrderService rentOrderService;
    @Resource
    private RentRenewService rentRenewService;
    @Resource
    private VipOrderService vipOrderService;
    @Resource
    private UserDepositService userDepositService;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;
    @Resource
    private UserWalletService userWalletService;

    @Override
    public void process(BusinessParam param) {
        BusinessType businessType = BusinessType.fromCode(param.getBusinessType());
        if (BusinessType.RENT_ORDER == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("租赁订单号不能为空");
            }
            java.math.BigDecimal payAmount = rentOrderService.prepareRentOrderPay(
                    param.getUserId(), param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        if (BusinessType.RENT_RENEW == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("续租单号不能为空");
            }
            java.math.BigDecimal payAmount = rentRenewService.prepareRenewPay(
                    param.getUserId(), param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        if (BusinessType.DEPOSIT_PAY == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("租赁押金支付单号不能为空");
            }
            java.math.BigDecimal payAmount = userDepositService.preparePay(
                    param.getUserId(), param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        if (BusinessType.MERCHANT_DEPOSIT_PAY == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("商户保证金支付单号不能为空");
            }
            java.math.BigDecimal payAmount = merchantSecurityDepositService.preparePay(
                    null, param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        if (BusinessType.WALLET_RECHARGE == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("充值支付单号不能为空");
            }
            java.math.BigDecimal payAmount = userWalletService.prepareRechargePay(
                    param.getUserId(), param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        if (BusinessType.VIP == businessType) {
            if (StringUtils.isBlank(param.getOrderNo())) {
                throw new BizException("会员订单号不能为空");
            }
            java.math.BigDecimal payAmount = vipOrderService.preparePay(
                    param.getUserId(), param.getOrderNo(), param.getAmount());
            param.setAmount(payAmount);
            return;
        }
        log.info("支付业务处理: {}", businessType.getCode());
    }
}

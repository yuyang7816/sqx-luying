package com.sqx.app.base.pay.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.domain.entity.RentRenewRecord;
import com.sqx.app.rental.order.mapper.RentRenewRecordMapper;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.app.rental.order.service.RentRenewService;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.app.wallet.wallet.service.UserWalletService;
import com.sqx.common.pay.notify.IPayNotifyBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
/**
 * 支付回调业务处理
 */
@Slf4j
@Service
public class PayNotifyBusinessServiceImpl implements IPayNotifyBusinessService {

    @Resource
    private RentOrderService rentOrderService;
    @Resource
    private RentRenewService rentRenewService;
    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentRenewRecordMapper rentRenewRecordMapper;
    @Resource
    private UserDepositService userDepositService;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;
    @Resource
    private UserWalletService userWalletService;
    @Resource
    private VipOrderService vipOrderService;

    @Override
    public void onPaymentSuccess(String channel, String outTradeNo, String tradeNo) {
        log.info("支付成功回调: channel={}, outTradeNo={}, tradeNo={}", channel, outTradeNo, tradeNo);
        if (StringUtils.isBlank(outTradeNo)) {
            return;
        }
        RentOrder order = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getOrderNo, outTradeNo)
                .last("LIMIT 1"));
        if (order != null) {
            rentOrderService.handlePaySuccess(outTradeNo, channel, tradeNo, order.getPayAmount());
            return;
        }
        RentRenewRecord renewRecord = rentRenewRecordMapper.selectOne(new LambdaQueryWrapper<RentRenewRecord>()
                .eq(RentRenewRecord::getRenewNo, outTradeNo)
                .last("LIMIT 1"));
        if (renewRecord != null) {
            rentRenewService.handleRenewPaySuccess(outTradeNo, channel, tradeNo, renewRecord.getRenewAmount());
            return;
        }
        if (outTradeNo.startsWith("DP")) {
            userDepositService.onDepositPaySuccess(null, outTradeNo, null);
            return;
        }
        if (outTradeNo.startsWith("MDP")) {
            merchantSecurityDepositService.onPaySuccess(null, outTradeNo, null);
            return;
        }
        if (outTradeNo.startsWith("WR")) {
            userWalletService.onRechargePaySuccess(null, outTradeNo, null);
            return;
        }
        if (outTradeNo.startsWith("VIP")) {
            vipOrderService.handlePaySuccess(outTradeNo, channel, tradeNo, null);
            return;
        }
        log.info("非租赁订单支付回调，跳过: {}", outTradeNo);
    }

    @Override
    public void onPaymentFailed(String channel, String outTradeNo, String tradeNo) {
        log.info("支付失败回调: channel={}, outTradeNo={}, tradeNo={}", channel, outTradeNo, tradeNo);
    }
}

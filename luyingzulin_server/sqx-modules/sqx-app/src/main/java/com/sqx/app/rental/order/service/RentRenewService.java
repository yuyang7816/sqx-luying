package com.sqx.app.rental.order.service;

import com.sqx.app.rental.order.domain.bo.RentRenewCreateBo;
import com.sqx.app.rental.order.domain.vo.RentRenewCreateVo;
import com.sqx.app.rental.order.domain.vo.RentRenewPreviewVo;

import java.math.BigDecimal;

/**
 * 续租服务
 */
public interface RentRenewService {

    /**
     * 续租预览
     */
    RentRenewPreviewVo preview(Long userId, Long orderId, String newEndTime);

    /**
     * 创建续租单（待支付）
     */
    RentRenewCreateVo create(Long userId, RentRenewCreateBo createBo);

    /**
     * 续租支付前校验
     */
    BigDecimal prepareRenewPay(Long userId, String renewNo, BigDecimal requestAmount);

    /**
     * 续租支付成功（延长租期，状态保持 renting）
     */
    void handleRenewPaySuccess(String renewNo, String payChannel, String payNo, BigDecimal paidAmount);
}

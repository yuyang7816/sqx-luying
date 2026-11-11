package com.sqx.app.rental.order.support;

import com.sqx.app.base.notification.core.enums.NotificationChannel;
import com.sqx.app.base.notification.core.model.NotificationRequest;
import com.sqx.app.base.notification.core.service.INotificationService;
import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.utils.wx.WxMaSubscribeMessage;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.common.redis.utils.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 租赁订单状态变更站内通知
 */
@Slf4j
@Component
public class RentOrderNotificationSupport {

    private static final int TYPE_ORDER = 3;
    private static final String CLIENT_TYPE_MA = "ma";
    private static final DateTimeFormatter SUBSCRIBE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private INotificationService notificationService;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private UserMapper userMapper;

    public void onPaySuccess(RentOrder order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        notifyUser(order.getUserId(), "支付成功",
                String.format("订单 %s 已支付成功，请按预约时间前往取货。", safeOrderNo(order)),
                order.getId());
        notifyMerchantOwner(order.getMerchantId(), "新订单待备货",
                String.format("订单 %s 已支付，请尽快完成备货。", safeOrderNo(order)),
                order.getId());
    }

    public void onCancel(RentOrder order, String reason) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        String detail = StringUtils.isBlank(reason) ? "您的租赁订单已取消。" : reason;
        notifyUser(order.getUserId(), "订单已取消",
                String.format("订单 %s %s", safeOrderNo(order), detail),
                order.getId());
    }

    public void onPrepared(RentOrder order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        notifyUser(order.getUserId(), "商品已备货",
                String.format("订单 %s 已完成备货，请携带取货凭证前往门店取货。", safeOrderNo(order)),
                order.getId());
    }

    public void onPickup(RentOrder order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        notifyUser(order.getUserId(), "租赁已开始",
                String.format("订单 %s 已核销取货，租期正式生效，请妥善使用并及时归还。", safeOrderNo(order)),
                order.getId());
    }

    public void onReturnApply(RentOrder order) {
        if (order == null) {
            return;
        }
        notifyUser(order.getUserId(), "归还申请已提交",
                String.format("订单 %s 归还申请已提交，请等待商户验收。", safeOrderNo(order)),
                order.getId());
        notifyMerchantOwner(order.getMerchantId(), "订单待验收",
                String.format("订单 %s 用户已申请归还，请尽快完成设备验收。", safeOrderNo(order)),
                order.getId());
    }

    public void onInspectionComplete(RentOrder order, BigDecimal deductAmount, BigDecimal refundAmount) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        BigDecimal deduct = deductAmount == null ? BigDecimal.ZERO : deductAmount;
        BigDecimal refund = refundAmount == null ? BigDecimal.ZERO : refundAmount;
        String content;
        if (deduct.compareTo(BigDecimal.ZERO) > 0) {
            content = String.format("订单 %s 验收完成，扣除押金 ¥%s，预计退还 ¥%s。如有疑问请联系客服。",
                    safeOrderNo(order), deduct.toPlainString(), refund.toPlainString());
        } else {
            content = String.format("订单 %s 验收完成，押金将原路退还（约 ¥%s）。", safeOrderNo(order), refund.toPlainString());
        }
        notifyUser(order.getUserId(), "订单已完成", content, order.getId());
    }

    private void notifyUser(Long userId, String title, String content, Long orderId) {
        send(userId, title, content, orderId, buildUserOrderUrl(orderId));
    }

    private void notifyMerchantOwner(Long merchantId, String title, String content, Long orderId) {
        if (merchantId == null) {
            return;
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getUserId() == null) {
            return;
        }
        send(merchant.getUserId(), title, content, orderId, buildMerchantOrderUrl(orderId));
    }

    private void send(Long userId, String title, String content, Long orderId, String url) {
        if (userId == null) {
            return;
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setTitle(title);
            request.setContent(content);
            request.setType(TYPE_ORDER);
            request.setDataId(orderId);
            request.setUrl(url);
            notificationService.send(request, NotificationChannel.INSTATION.code());
            attachSubscribeMessage(request, userId, title, content, orderId, url);
            notificationService.send(request, NotificationChannel.MA.code());
        } catch (Exception e) {
            log.warn("订单通知发送失败 userId={} orderId={}: {}", userId, orderId, e.getMessage());
        }
    }

    private void attachSubscribeMessage(NotificationRequest request, Long userId, String title,
                                        String content, Long orderId, String url) {
        List<String> templateIds = loadOrderSubscribeTemplateIds();
        if (templateIds.isEmpty()) {
            return;
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null || StringUtils.isBlank(user.getMaOpenId())) {
            return;
        }
        WxMaSubscribeMessage message = new WxMaSubscribeMessage();
        message.setToUser(user.getMaOpenId());
        message.setTemplateId(templateIds.get(0));
        message.setPage(StringUtils.defaultIfBlank(trimSubscribePage(url), "packageOrder/list"));
        List<WxMaSubscribeMessage.MsgData> data = new ArrayList<>();
        data.add(new WxMaSubscribeMessage.MsgData("thing1", truncate(title, 20)));
        data.add(new WxMaSubscribeMessage.MsgData("thing2", truncate(content, 20)));
        data.add(new WxMaSubscribeMessage.MsgData("time3", LocalDateTime.now().format(SUBSCRIBE_TIME_FMT)));
        if (orderId != null) {
            data.add(new WxMaSubscribeMessage.MsgData("character_string4", String.valueOf(orderId)));
        }
        message.setData(data);
        request.setWxMaSubscribeMessage(message);
        request.setClientType(CLIENT_TYPE_MA);
    }

    private List<String> loadOrderSubscribeTemplateIds() {
        String raw = ConfigUtil.getConfigValue(SysConfigConstant.Sys.SYS_USER_MA_SUBSCRIBE_ORDER_NOTIFY);
        if (StringUtils.isBlank(raw)) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private static String trimSubscribePage(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        return url.startsWith("/") ? url.substring(1) : url;
    }

    private static String truncate(String text, int maxLen) {
        String value = StringUtils.defaultString(text, "");
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private static String buildUserOrderUrl(Long orderId) {
        return orderId == null ? null : "/packageOrder/detail?id=" + orderId;
    }

    private static String buildMerchantOrderUrl(Long orderId) {
        return orderId == null ? null : "/packageMerchant/order-detail?id=" + orderId;
    }

    private static String safeOrderNo(RentOrder order) {
        return order.getOrderNo() == null ? String.valueOf(order.getId()) : order.getOrderNo();
    }
}

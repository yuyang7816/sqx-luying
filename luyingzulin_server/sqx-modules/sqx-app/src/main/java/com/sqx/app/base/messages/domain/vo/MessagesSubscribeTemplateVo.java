package com.sqx.app.base.messages.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "小程序订阅消息模板")
public class MessagesSubscribeTemplateVo {

    @Schema(description = "订单状态通知模板 ID 列表")
    private List<String> orderNotify;
}

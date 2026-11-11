package com.sqx.app.base.im.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IM 消息
 */
@Getter
@Setter
@TableName("im_message")
public class ImMessage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    /** user / merchant / system */
    @TableField("sender_role")
    private String senderRole;

    @TableField("sender_user_id")
    private Long senderUserId;

    /** text / video / image */
    @TableField("msg_type")
    private String msgType;

    @TableField("content")
    private String content;

    @TableField("media_url")
    private String mediaUrl;

    @TableField("order_id")
    private Long orderId;

    @TableField("goods_id")
    private Long goodsId;

    /** tutorial_video 等 */
    @TableField("biz_type")
    private String bizType;

    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

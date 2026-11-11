package com.sqx.app.base.platformcs.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("platform_cs_message")
public class PlatformCsMessage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_role")
    private String senderRole;

    @TableField("sender_user_id")
    private Long senderUserId;

    @TableField("sender_admin_id")
    private Long senderAdminId;

    @TableField("msg_type")
    private String msgType;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

package com.sqx.app.rental.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户意见反馈
 */
@Getter
@Setter
@TableName("user_feedback")
public class UserFeedback implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("feedback_no")
    private String feedbackNo;

    @TableField("user_id")
    private Long userId;

    @TableField("feedback_type")
    private String feedbackType;

    @TableField("content")
    private String content;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("status")
    private String status;

    @TableField("reply_remark")
    private String replyRemark;

    @TableField("process_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}

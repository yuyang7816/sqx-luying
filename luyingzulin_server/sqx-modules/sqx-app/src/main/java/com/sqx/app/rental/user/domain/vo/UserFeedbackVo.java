package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserFeedbackVo {

    private Long id;
    private String feedbackNo;
    private Long userId;
    @Schema(description = "手机号（管理端明文）")
    private String phone;
    private String phoneMasked;
    private String userName;
    private String feedbackType;
    private String feedbackTypeLabel;
    private String content;
    private String contactPhone;
    private List<String> imageUrls;
    private String status;
    private String statusLabel;
    private String replyRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

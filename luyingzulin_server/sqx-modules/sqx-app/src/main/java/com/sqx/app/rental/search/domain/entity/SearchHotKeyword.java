package com.sqx.app.rental.search.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 热门搜索词
 */
@Getter
@Setter
@TableName("search_hot_keyword")
@Schema(name = "SearchHotKeyword", description = "热门搜索词")
public class SearchHotKeyword implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "搜索关键词")
    @TableField("keyword")
    private String keyword;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "状态：1启用 0禁用")
    @TableField("status")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}

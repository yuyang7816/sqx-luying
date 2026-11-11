package com.sqx.app.base.appConfig.controller;

import com.sqx.app.base.appConfig.domain.bo.SysConfigInfoBO;
import com.sqx.app.base.appConfig.domain.vo.SysConfigVO;
import com.sqx.app.base.appConfig.service.AppConfigService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author chengming
 * @since 2025/6/9
 */
@Tag(name = "用户端-系统配置")
@RestController
@RequestMapping("app/config")
public class AppConfigController {

    @Resource
    private AppConfigService appConfigService;

    @Operation(summary = "系统配置信息")
    @PostMapping("info")
    public Result<SysConfigVO> info(@RequestBody SysConfigInfoBO sysConfigInfoBO) {
        SysConfigVO vo = appConfigService.info(sysConfigInfoBO);
        return Result.success(vo);
    }
}

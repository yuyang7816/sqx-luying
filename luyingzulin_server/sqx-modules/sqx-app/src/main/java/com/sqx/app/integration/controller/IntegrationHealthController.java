package com.sqx.app.integration.controller;

import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 三端联调健康探测（无 spring-boot-actuator 时的轻量替代）
 * <p>
 * 探测 URL：{@code GET /sqx_fast/health}，勿使用无 context-path 的 {@code /actuator/health}。
 */
@Tag(name = "联调-健康检查", description = "三端联调冒烟探测")
@RestController
@RequestMapping("/health")
public class IntegrationHealthController {

    @Operation(summary = "服务健康状态", description = "返回 UP 供联调脚本与自动化探测")
    @GetMapping
    public Result<Map<String, String>> health() {
        return Result.success(Collections.singletonMap("status", "UP"));
    }
}

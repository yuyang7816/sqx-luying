package com.sqx.runner;

import com.sqx.common.pay.channel.wechat.manager.WxPayServiceManager;
import com.sqx.system.db.service.ISysConfigService;
import com.sqx.system.service.SysMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chengming
 * @since 2024/7/23
 */
@Slf4j
@Component
public class SqxApplicationRunner implements ApplicationRunner {
    @Resource
    private ISysConfigService sysConfigService;
    @Resource
    private SysMenuService sysMenuService;
    @Resource
    private WxPayServiceManager wxPayServiceManager;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("初始化系统配置...");
            sysConfigService.loadSysConfig();
        } catch (Exception ex) {
            log.warn("系统配置初始化跳过: {}", ex.getMessage());
        }

        try {
            log.info("缓存系统菜单");
            sysMenuService.loadSysMenu();
        } catch (Exception ex) {
            log.warn("系统菜单缓存跳过: {}", ex.getMessage());
        }

        try {
            log.info("加载微信客户端");
            wxPayServiceManager.initializeWxPayServices();
        } catch (Exception ex) {
            log.warn("微信客户端加载跳过: {}", ex.getMessage());
        }
    }
}

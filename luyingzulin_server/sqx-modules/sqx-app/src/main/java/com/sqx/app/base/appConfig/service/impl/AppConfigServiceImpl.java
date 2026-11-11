package com.sqx.app.base.appConfig.service.impl;

import com.sqx.app.base.appConfig.domain.bo.SysConfigInfoBO;
import com.sqx.app.base.appConfig.domain.vo.SysConfigVO;
import com.sqx.app.base.appConfig.service.AppConfigService;
import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.merchant.constants.MerchantConstant;
import com.sqx.app.rental.user.constants.UserDepositConstant;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.exception.SqxException;
import com.sqx.system.db.entity.SysConfigEntity;
import com.sqx.system.db.service.ISysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chengming
 * @since 2025/6/9
 */
@Service
public class AppConfigServiceImpl implements AppConfigService {

    private static final Map<String, String> OPTIONAL_DEFAULTS;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put(SysConfigConstant.Sys.SYS_OA_OAUTH_ENABLED, "0");
        defaults.put(MerchantConstant.securityDeposit.MERCHANT_SECURITY_DEPOSIT_AMOUNT, "0");
        defaults.put(UserDepositConstant.config.USER_MIN_DEPOSIT_BALANCE, "0");
        OPTIONAL_DEFAULTS = Collections.unmodifiableMap(defaults);
    }

    @Resource
    private ISysConfigService iSysConfigService;

    @Override
    public SysConfigVO info(SysConfigInfoBO sysConfigInfoBO) {
        String key = sysConfigInfoBO.getKey();
        String configValue;
        try {
            configValue = iSysConfigService.getConfigValue(key);
        } catch (SqxException ex) {
            configValue = readConfigValueFromDb(key);
            if (configValue == null) {
                configValue = OPTIONAL_DEFAULTS.get(key);
            }
            if (configValue == null) {
                throw new BizException(AppError.CONFIG_NOT_FOUND);
            }
        }

        SysConfigVO vo = new SysConfigVO();
        vo.setValue(configValue);
        return vo;
    }

    private String readConfigValueFromDb(String key) {
        SysConfigEntity config = iSysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getParamKey, key)
                .last("LIMIT 1"));
        if (config == null || StringUtils.isBlank(config.getParamValue())) {
            return null;
        }
        return config.getParamValue();
    }
}

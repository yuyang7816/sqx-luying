package com.sqx.app.base.utils.wx;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WxMaConfigSupportTest {

    @Test
    void resolveSecretShouldPreferJsapiSecretWhenAppIdsMatch() throws Exception {
        Method method = WxMaConfigSupport.class.getDeclaredMethod(
                "resolveSecret", String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        String secret = (String) method.invoke(null,
                "wx123", "wx123", "old-wrong-secret", "wx123", "correct-secret");
        assertEquals("correct-secret", secret);
    }

    @Test
    void resolveSecretShouldKeepMaSecretWhenJsapiAppIdDiffers() throws Exception {
        Method method = WxMaConfigSupport.class.getDeclaredMethod(
                "resolveSecret", String.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        String secret = (String) method.invoke(null,
                "wx123", "wx123", "ma-secret", "wx999", "jsapi-secret");
        assertEquals("ma-secret", secret);
    }
}

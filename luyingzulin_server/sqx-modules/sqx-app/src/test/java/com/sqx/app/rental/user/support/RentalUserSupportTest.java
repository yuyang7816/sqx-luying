package com.sqx.app.rental.user.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M05：用户 VO 辅助方法单元测试
 */
class RentalUserSupportTest {

    @Test
    void maskPhoneShouldHideMiddleDigits() {
        assertEquals("138****8888", RentalUserSupport.maskPhone("13812348888"));
    }

    @Test
    void buildFullAddressShouldConcatenateRegionAndDetail() {
        String full = RentalUserSupport.buildFullAddress("陕西省", "西安市", "雁塔区", "科技路 198 号");
        assertTrue(full.contains("陕西省"));
        assertTrue(full.contains("科技路 198 号"));
    }
}

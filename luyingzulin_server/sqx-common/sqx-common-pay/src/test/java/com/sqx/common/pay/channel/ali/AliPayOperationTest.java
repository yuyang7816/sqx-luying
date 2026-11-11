package com.sqx.common.pay.channel.ali;

import com.sqx.common.pay.enums.PayTradeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliPayOperationTest {

    @Test
    void formatAmountShouldKeepTwoDecimals() {
        assertEquals("183.80", AliPayOperation.formatAmount(new BigDecimal("183.8")));
    }

    @Test
    void payTradeTypeFromCodeShouldAcceptH5() {
        assertEquals(PayTradeType.H5, PayTradeType.fromCode("H5"));
    }
}

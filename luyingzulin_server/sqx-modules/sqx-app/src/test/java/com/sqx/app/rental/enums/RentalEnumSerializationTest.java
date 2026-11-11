package com.sqx.app.rental.enums;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqx.app.base.pay.enums.BusinessType;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * M00：租赁相关枚举 fromCode 与 JSON 序列化单元测试
 */
class RentalEnumSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void orderStatusFromCodeRoundTrip(OrderStatus status) {
        assertEquals(status, OrderStatus.fromCode(status.getCode()));
        assertEquals("\"" + status.name() + "\"", JSON.toJSONString(status));
    }

    @ParameterizedTest
    @EnumSource(RentType.class)
    void rentTypeFromCodeRoundTrip(RentType rentType) {
        assertEquals(rentType, RentType.fromCode(rentType.getCode()));
    }

    @ParameterizedTest
    @EnumSource(PickupType.class)
    void pickupTypeFromCodeRoundTrip(PickupType pickupType) {
        assertEquals(pickupType, PickupType.fromCode(pickupType.getCode()));
    }

    @ParameterizedTest
    @EnumSource(PayBizType.class)
    void payBizTypeFromCodeRoundTrip(PayBizType payBizType) {
        assertEquals(payBizType, PayBizType.fromCode(payBizType.getCode()));
    }

    @ParameterizedTest
    @EnumSource(BusinessType.class)
    void businessTypeFromCodeRoundTrip(BusinessType businessType) throws Exception {
        assertEquals(businessType, BusinessType.fromCode(businessType.getCode()));
        assertEquals("\"" + businessType.name() + "\"", objectMapper.writeValueAsString(businessType));
    }

    @Test
    void invalidCodeThrowsBizException() {
        assertThrows(BizException.class, () -> OrderStatus.fromCode("unknown_status"));
        assertThrows(BizException.class, () -> RentType.fromCode("unknown_rent"));
        assertThrows(BizException.class, () -> PickupType.fromCode("unknown_pickup"));
        assertThrows(BizException.class, () -> PayBizType.fromCode("unknown_pay"));
        assertThrows(BizException.class, () -> BusinessType.fromCode("unknown_biz"));
    }
}

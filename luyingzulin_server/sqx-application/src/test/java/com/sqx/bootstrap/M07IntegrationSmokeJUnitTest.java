package com.sqx.bootstrap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * JUnit 包装 M07 三端联调冒烟，便于 mvn -Pe2e -Dtest=M07IntegrationSmokeJUnitTest 执行。
 */
@Tag("e2e")
class M07IntegrationSmokeJUnitTest {

    @Test
    void runIntegrationSmoke() throws Exception {
        M07IntegrationSmokeTest.main(new String[0]);
    }
}

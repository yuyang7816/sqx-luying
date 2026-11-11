package com.sqx.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * M07 联调前置：幂等创建 merchant 表并写入演示商户种子。
 * 执行：mvn -pl sqx-application -am test -Pe2e -Dtest=M07SeedBootstrapTest
 */
@Tag("e2e")
class M07SeedBootstrapTest {

    @Test
    void ensureMerchantSchemaAndSeed() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "跳过：MySQL 不可达");
        E2eM07SeedSupport.ensureMerchantSeedIfMissing();
        assertTrue(E2eServerSupport.isAdminLoginAvailable());
    }
}

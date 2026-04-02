package com.aibookkeeping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class AibookkeepingApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常加载
    }
}

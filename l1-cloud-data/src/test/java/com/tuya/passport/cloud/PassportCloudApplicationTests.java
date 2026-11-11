package com.tuya.passport.cloud;

import org.junit.jupiter.api.Test;
import com.tuya.passport.cloud.PassportCloudApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = PassportCloudApplication.class)
@ActiveProfiles("test")
class PassportCloudApplicationTests {
    @Test void contextLoads() {}
}

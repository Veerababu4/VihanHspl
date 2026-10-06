package com.vihan.hospital;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Shared setup for integration tests. Every class using it shares one Spring context and one
 * Oracle container, so the database starts only once per test run.
 * The {@code local} profile loads the fictional sample data, so that script is tested too.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = "app.upload-dir=target/test-uploads")
public @interface VihanIntegrationTest {
}

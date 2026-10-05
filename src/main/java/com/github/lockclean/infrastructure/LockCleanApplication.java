package com.github.lockclean.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. It lives at the root of the {@code infrastructure} package on purpose:
 * component scanning starts here and therefore never reaches {@code core}, which stays
 * framework-free. The application has no driving adapter of its own — the use cases are exercised
 * from the integration tests, which assemble them with the adapters this context provides.
 */
@SpringBootApplication
public class LockCleanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LockCleanApplication.class, args);
    }
}

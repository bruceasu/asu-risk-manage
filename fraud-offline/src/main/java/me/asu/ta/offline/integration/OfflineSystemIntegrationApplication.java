package me.asu.ta.offline.integration;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main application class for the offline system integration.
 * This class is responsible for bootstrapping the Spring application context
 * without starting a web server, as this is an offline processing application.
 */
@SpringBootApplication(scanBasePackages = "me.asu.ta")
public class OfflineSystemIntegrationApplication {
    public static ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(OfflineSystemIntegrationApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties(
                        "spring.main.banner-mode=off",
                        "spring.main.lazy-initialization=true")
                .run();
    }
}

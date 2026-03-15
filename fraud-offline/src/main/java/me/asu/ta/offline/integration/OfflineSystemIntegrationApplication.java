package me.asu.ta.offline.integration;

import me.asu.ta.offline.OfflineReplayFacade;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

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

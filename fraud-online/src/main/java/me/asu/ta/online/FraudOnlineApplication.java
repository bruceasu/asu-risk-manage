package me.asu.ta.online;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "me.asu.ta")
public class FraudOnlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudOnlineApplication.class, args);
    }
}

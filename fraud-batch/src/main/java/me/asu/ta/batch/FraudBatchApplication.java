package me.asu.ta.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "me.asu.ta")
public class FraudBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudBatchApplication.class, args);
    }
}

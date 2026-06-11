package com.blombank.cvautomation;

import com.blombank.cvautomation.services.EmailListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CvAutomationApplication {

    private static final Logger logger = LoggerFactory.getLogger(CvAutomationApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CvAutomationApplication.class, args);
        int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
        System.exit(exitCode);
    }

    @Bean
    public CommandLineRunner run(EmailListenerService emailListenerService) {
        return args -> {
            logger.info("BLOM Bank CV Automation Batch — starting...");
            emailListenerService.processInbox();
            logger.info("BLOM Bank CV Automation Batch — completed.");
        };
    }

}

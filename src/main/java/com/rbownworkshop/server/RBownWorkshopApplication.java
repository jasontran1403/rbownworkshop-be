package com.rbownworkshop.server;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@RequiredArgsConstructor
@EnableScheduling
@Log4j2
public class RBownWorkshopApplication {
	public static void main(String[] args) {
		SpringApplication.run(RBownWorkshopApplication.class, args);
	}

	@Scheduled(cron = "0 */10 * * * *")
	@Transactional
	public void cronjob() {
	}
}

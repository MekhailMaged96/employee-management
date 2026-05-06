package com.example.employee_management_system.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

	private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

	@Async
	public CompletableFuture<String> runAsyncTask(String name) {
		log.info("Async task started for {}", name);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Async task interrupted for {}", name);
			return CompletableFuture.completedFuture("error:" + name);
		}
		log.info("Async task finished for {}", name);
		return CompletableFuture.completedFuture("done:" + name);
	}


    @Async
    public CompletableFuture<String> SendEmail(){

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Email sending interrupted");
            return CompletableFuture.completedFuture("error: email");
        }
        return CompletableFuture.completedFuture("Email sent successfully");
    }


}



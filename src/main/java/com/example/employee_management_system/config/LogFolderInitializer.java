package com.example.employee_management_system.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class LogFolderInitializer {

	private static final Logger log = LoggerFactory.getLogger(LogFolderInitializer.class);

	private static final String LOG_DIR = "log"; // relative to working dir

	@EventListener(ApplicationReadyEvent.class)
	public void createLogFolder() {
		Path path = Path.of(LOG_DIR);
		try {
			if (Files.exists(path)) {
				log.debug("Log directory already exists: {}", path.toAbsolutePath());
			} else {
				Files.createDirectories(path);
				log.info("Created log directory: {}", path.toAbsolutePath());
			}
		} catch (IOException e) {
			log.error("Failed to create log directory {}", path.toAbsolutePath(), e);
		}
	}
}

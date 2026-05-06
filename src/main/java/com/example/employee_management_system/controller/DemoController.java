package com.example.employee_management_system.controller;

import com.example.employee_management_system.service.AsyncService;
import com.example.employee_management_system.service.SchedulingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {

	private static final Logger log = LoggerFactory.getLogger(DemoController.class);

	@Autowired
	private AsyncService asyncService;

	@Autowired
	private SchedulingService schedulingService;

	@GetMapping("/schedule/count")
	public ResponseEntity<Map<String, Integer>> getScheduleCount() {
		return ResponseEntity.ok(Map.of("count", schedulingService.getCount()));
	}

	@PostMapping("/async")
	public ResponseEntity<Map<String, String>> triggerAsync(@RequestParam(defaultValue = "anonymous") String name) {
		asyncService.runAsyncTask(name);
		log.info("Accepted async request for {}", name);
		return ResponseEntity.ok(Map.of("status", "accepted"));
	}

	@GetMapping("/async/sync")
	public ResponseEntity<String> triggerAsyncAndWait(@RequestParam(defaultValue = "anonymous") String name) {
		String result = asyncService.runAsyncTask(name).join();
		return ResponseEntity.ok(result);
	}
}



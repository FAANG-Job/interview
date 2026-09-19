package com.faangjob.interview;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class HealthController {
	private static final Logger logger =
            Logger.getLogger(HealthController.class.getName());
    @GetMapping("/health")
    public Map<String, String> health() {
		logger.info("Health-check endpoint called");
        return Map.of("status", "UP");

    }
}

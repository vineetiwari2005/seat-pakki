package com.driver.bookMyShow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduled tasks for seat lock cleanup
@EnableAsync // Enable async method execution for email service
public class BookMyShowApplication {

	public static void main(String[] args) {
		overrideSanitizedDbProperty("DB_URL");
		overrideSanitizedDbProperty("DB_USERNAME");
		overrideSanitizedDbProperty("DB_PASSWORD");
		SpringApplication.run(BookMyShowApplication.class, args);
	}

	private static void overrideSanitizedDbProperty(String key) {
		String rawValue = System.getenv(key);
		if (rawValue == null) {
			return;
		}

		String sanitized = rawValue.trim();
		if (sanitized.length() >= 2) {
			char first = sanitized.charAt(0);
			char last = sanitized.charAt(sanitized.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				sanitized = sanitized.substring(1, sanitized.length() - 1).trim();
			}
		}

		System.setProperty(key, sanitized);
	}

//TODO:
//	3. Get count of unique locations of a theater
//	4. Get the list of theaters Showing a particular time.
//	6. Cancel Ticket
//	8. rate movie Flop or Hit based on collection or ticketBooked
}

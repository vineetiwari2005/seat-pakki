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
		SpringApplication.run(BookMyShowApplication.class, args);
	}

//TODO:
//	3. Get count of unique locations of a theater
//	4. Get the list of theaters Showing a particular time.
//	6. Cancel Ticket
//	8. rate movie Flop or Hit based on collection or ticketBooked
}

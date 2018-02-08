package cz.hystrix.bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HystrixBugApplication {

	public static void main(String[] args) {
		SpringApplication.run(HystrixBugApplication.class, args);
	}
}

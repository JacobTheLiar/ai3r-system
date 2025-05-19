package pl.jit.robotsystem.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.jit.robotsystem.demo.service.HackLoginService;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

    @Bean
    public CommandLineRunner run(HackLoginService service) {
        return args -> {
            service.action();
            System.exit(0);
        };
    }
}
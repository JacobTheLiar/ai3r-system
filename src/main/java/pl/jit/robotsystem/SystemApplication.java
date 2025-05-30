package pl.jit.robotsystem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pl.jit.robotsystem.service.ConnectionsService;

@SpringBootApplication
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(ConnectionsService service) {
        return args -> {
            service.action();
            System.exit(0);
        };
    }
}
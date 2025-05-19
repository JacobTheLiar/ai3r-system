package pl.jit.robotsystem.configuration;

import lombok.extern.java.Log;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

@Configuration
@Log
public class FileSystemConfiguration {
    @Bean
    public File workingDirectoryRoot(){
        File workingDir = new File("working-directory");
        if (!workingDir.isDirectory()) {
            try {
                return Files.createDirectories(workingDir.toPath()).toFile();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error creating working directory", e);
                throw new RuntimeException(e);
            }
        }
        return workingDir;
    }
}

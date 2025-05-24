package pl.jit.robotsystem.configuration;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConverterConfiguration {

    @Bean
    public FlexmarkHtmlConverter converter() {
        return FlexmarkHtmlConverter.builder().build();
    }
}

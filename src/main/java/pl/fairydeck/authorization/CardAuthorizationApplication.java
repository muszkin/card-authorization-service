package pl.fairydeck.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CardAuthorizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardAuthorizationApplication.class, args);
    }
}

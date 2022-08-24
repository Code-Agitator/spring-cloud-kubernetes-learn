package example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "example")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @LoadBalanced
    @Bean
    RestTemplate loadBalancedWebClientBuilder() {
        return new RestTemplateBuilder().build();
    }
}

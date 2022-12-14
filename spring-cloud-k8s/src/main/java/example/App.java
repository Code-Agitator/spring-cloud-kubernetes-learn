package example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
@EnableConfigurationProperties(AppProperties.class)
public class App {
    @Resource
    DiscoveryClient discoveryClient;

    @Resource
    AppProperties appProperties;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/")
    public String hello() {
        return "Hello World";
    }

    @GetMapping("/services")
    public List<String> getServices() {
        return discoveryClient.getServices();
    }


    @GetMapping("/config")
    public String getMessage() {
        return appProperties.getMessage();
    }
}

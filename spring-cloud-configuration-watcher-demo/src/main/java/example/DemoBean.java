package example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@EnableScheduling
public class DemoBean {

    @Autowired
    private DemoProperties config;

    public String hello() {
        System.out.println((new Date()).toString() + ": Now processing on " + config.getMessage());
        return config.getMessage();
    }
}
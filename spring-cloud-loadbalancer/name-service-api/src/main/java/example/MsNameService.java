package example;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "name-service")
public interface MsNameService {
    @GetMapping("/name")
    String getName(@RequestParam(value = "delay", defaultValue = "0") int delayValue);
}

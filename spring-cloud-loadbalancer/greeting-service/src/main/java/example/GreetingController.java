package example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {


    private final MsNameService msNameService;

    public GreetingController(MsNameService msNameService) {
        this.msNameService = msNameService;
    }

    @GetMapping("/greeting")
    public String getGreeting(@RequestParam(value = "delay", defaultValue = "0") int delay) {
        return String.format("Hello from %s!", msNameService.getName(delay));
    }

}
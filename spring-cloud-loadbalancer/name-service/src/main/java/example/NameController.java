package example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NameController {

	private static final Logger LOG = LoggerFactory.getLogger(NameController.class);

	private final String hostName = System.getenv("HOSTNAME");

	@GetMapping("/")
	public String ribbonPing() {
		LOG.info("Ribbon ping");
		return hostName;
	}

	/**
	 * 返回hostname  并模拟延时
	 */
	@GetMapping("/name")
	public String getName(@RequestParam(value = "delay", defaultValue = "0") int delayValue) {
		LOG.info(String.format("Returning a name '%s' with a delay '%d'", hostName, delayValue));
		delay(delayValue);
		return hostName;
	}

	private void delay(int delayValue) {
		try {
			Thread.sleep(delayValue);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

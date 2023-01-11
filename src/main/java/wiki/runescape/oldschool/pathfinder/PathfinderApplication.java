package wiki.runescape.oldschool.pathfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PathfinderApplication {

	public static void main(String[] args) {
		SpringApplication.run(PathfinderApplication.class, args);
		final Logger logger = LoggerFactory.getLogger(PathfinderApplication.class);
		logger.info("Go to http://localhost:8080/frontend/index.html?m=-1&z=3&p=0&x=3229&y=3216  :)");
	}

}

package wiki.runescape.oldschool.pathfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PathfinderApplication {

	public static void main(String[] args) {
		SpringApplication.run(PathfinderApplication.class, args);
		System.out.println("Go to http://localhost:8080/frontend/index.html?m=-1&z=3&p=0&x=3229&y=3216     :)");
	}

}

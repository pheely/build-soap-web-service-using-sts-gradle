package hello;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import io.pheely.get_movie_web_service.Movie;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class MovieRepository {
	private static final Map<String, Movie> movies = new HashMap<>();

	@PostConstruct
	public void initData() {
		Movie titanic = new Movie();
		titanic.setName("Titanic");
		titanic.setYear(1997);
		titanic.setCountry("USA");
		titanic.setGenra("epic romance-disaster");
		titanic.setDirector("James Cameron");

		movies.put(titanic.getName(), titanic);

		Movie pearlHarbor = new Movie();
		pearlHarbor.setName("Pearl Harbor");
		pearlHarbor.setYear(2001);
		pearlHarbor.setCountry("USA");
		pearlHarbor.setGenra("romantic period war drama");
		pearlHarbor.setDirector("Michael Bay");

		movies.put(pearlHarbor.getName(), pearlHarbor);

		Movie spectre = new Movie();
		spectre.setName("Spectre");
		spectre.setYear(2015);
		spectre.setCountry("USA");
		spectre.setGenra("spy");
		spectre.setDirector("Sam Mendes");

		movies.put(spectre.getName(), spectre);
	}

	public Movie findMovie(String name) {
		Assert.notNull(name, "The movie's name must not be null");
		return movies.get(name);
	}
}

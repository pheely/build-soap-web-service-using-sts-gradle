package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import io.pheely.get_movie_web_service.GetMovieRequest;
import io.pheely.get_movie_web_service.GetMovieResponse;

@Endpoint
public class MovieEndpoint {
	private static final String NAMESPACE_URI = "http://pheely.io/get-movie-web-service";

	private MovieRepository movieRepository;

	@Autowired
	public MovieEndpoint(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getMovieRequest")
	@ResponsePayload
	public GetMovieResponse getMovie(@RequestPayload GetMovieRequest request) {
		GetMovieResponse response = new GetMovieResponse();
		response.setMovie(movieRepository.findMovie(request.getName()));

		return response;
	}
}
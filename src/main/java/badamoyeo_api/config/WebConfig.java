package badamoyeo_api.config;

import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final Path uploadDirectory;
	private final String[] allowedOrigins;

	public WebConfig(
		@Value("${app.upload.directory:uploads}") String uploadDirectory,
		@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins
	) {
		this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toArray(String[]::new);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/**")
			.addResourceLocations(uploadDirectory.toUri().toString() + "/");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins(allowedOrigins)
			.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(true);
	}
}

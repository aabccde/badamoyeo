package badamoyeo_api.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiPrefixForwardFilter extends OncePerRequestFilter {
	private static final String API_PREFIX = "/api";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String path = request.getRequestURI();
		if (path.equals(API_PREFIX)) {
			request.getRequestDispatcher("/").forward(request, response);
			return;
		}
		if (path.startsWith(API_PREFIX + "/")) {
			request.getRequestDispatcher(path.substring(API_PREFIX.length())).forward(request, response);
			return;
		}

		filterChain.doFilter(request, response);
	}
}

package pmhsfelix.async.examples;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TheFilter extends OncePerRequestFilter {

    private static final Logger logger = LogManager.getLogger(TheFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean isFirstRequest = !this.isAsyncDispatch(request);
        logger.info("filter: before");
        filterChain.doFilter(request, response);
        logger.info("filter: after");
    }

    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}


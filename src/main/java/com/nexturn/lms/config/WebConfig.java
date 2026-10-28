package com.nexturn.lms.config;
import com.nexturn.lms.service.LoginService;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
/** Authenticates API calls before controller execution and limits browser origins explicitly. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginService loginService;
    private final String[] origins;
    public WebConfig(LoginService loginService, @Value("${app.cors.origins:http://localhost:5173}") String[] origins) {
        this.loginService = loginService; this.origins = origins;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("X-Content-Type-Options", "nosniff");
                if ("OPTIONS".equals(request.getMethod())) return true;
                request.setAttribute("authenticatedUser", loginService.authenticate(request.getHeader("Authorization")));
                return true;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type").maxAge(3600);
    }
}

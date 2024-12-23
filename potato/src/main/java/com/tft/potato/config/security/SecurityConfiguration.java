package com.tft.potato.config.security;

import com.tft.potato.config.SkipPathList;
import com.tft.potato.config.security.filter.JwtAuthenticationFilter;
import com.tft.potato.config.security.handler.CustomLogoutSuccessHandler;
import com.tft.potato.config.security.handler.SuccessLoginHandler;
import com.tft.potato.config.security.provider.JwtProvider;
import com.tft.potato.config.security.service.CustomOAuth2UserService;
import com.tft.potato.config.security.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.List.*;

@Configuration
//@EnableWebSecurity(debug = true)
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final SuccessLoginHandler successLoginHandler;
    private final TokenService tokenService;
    //private final FailLoginHandler failLoginHandler;


    @Bean
    public CustomLogoutSuccessHandler customLogoutSuccessHandler() {
        return new CustomLogoutSuccessHandler();
    }

    @Bean
    public JwtAuthenticationFilter getJwtAuthenticationFilter() throws Exception{
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtProvider(), tokenService);
        return filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String[] permitPaths = SkipPathList.getPermitAllPaths().toArray(new String[0]);
        log.info("===> Auth Permit Paths : " + Arrays.deepToString(permitPaths));

        http
                .cors().and()// cors 활성화
                .csrf().disable()
                .formLogin().disable()
                .rememberMe().disable()
                .httpBasic().disable()
                .authorizeRequests()
                .antMatchers(permitPaths).permitAll()
                .anyRequest().authenticated()

                .and()
                .addFilterAfter(getJwtAuthenticationFilter(), WebAsyncManagerIntegrationFilter.class)

                .logout()
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies()
                .clearAuthentication(true)
                .logoutSuccessHandler(customLogoutSuccessHandler())

                .and()
                .oauth2Login()
                .successHandler(successLoginHandler)
                //.failureHandler(failLoginHandler)
                .userInfoEndpoint()
                .userService(customOAuth2UserService)

        ;

        return http.build();
    }

    /*
        <커스텀된 필터들을 추가하는 영역>
        Security filter chain: [
              DisableEncodeUrlFilter
              WebAsyncManagerIntegrationFilter
              JwtAuthenticationFilter ->
              SecurityContextPersistenceFilter -> 각 요청마다 Context를 다시 만들기 때문에 JWT 검증을 이거 이전에 한다.
              HeaderWriterFilter
              LogoutFilter
              OAuth2AuthorizationRequestRedirectFilter
              OAuth2LoginAuthenticationFilter -> 인증 전에 처리가 필요하면 이거 전에 추가한다.
              DefaultLoginPageGeneratingFilter
              DefaultLogoutPageGeneratingFilter
              RequestCacheAwareFilter
              SecurityContextHolderAwareRequestFilter
              AnonymousAuthenticationFilter
              SessionManagementFilter
              ExceptionTranslationFilter
              FilterSecurityInterceptor -> 로깅이나 모니터링용 필터는 이거 뒤에 추가한다
        ]
     */

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowCredentials(true);
        configuration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

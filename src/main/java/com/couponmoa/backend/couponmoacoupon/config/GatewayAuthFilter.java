package com.couponmoa.backend.couponmoacoupon.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {  // 이후에 공통 common모듈로 라이브러리화 예정

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");

        String role = request.getHeader("X-User-Role");

        if (userId != null && role != null) {

            var authorities = List.of(new SimpleGrantedAuthority(role));

            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication object set for user: {}, roles: {}", userId, authorities);
        } else {
            log.debug("X-User-Id or X-User-Role header not found or is null for path: {}", request.getRequestURI());
        }

        chain.doFilter(request, response);
    }
}
package com.bilangieri.agendamento.security;

import com.bilangieri.agendamento.auth.service.JwtService;
import com.bilangieri.agendamento.user.service.AccountUserDetails;
import com.bilangieri.agendamento.user.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Autentica o pedido a partir do header "Authorization: Bearer <token>". Um token inválido não autentica
// ninguém: o pedido segue sem utilizador e a SecurityConfig responde 401 se o endpoint o exigir.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authHeader.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            // A conta é lida a cada pedido: desativar um utilizador corta o acesso mesmo com um token ainda válido.
            // Mudar o email ou a password invalida os tokens emitidos antes (as outras sessões caem)
            Claims claims = jwtService.extractClaims(token);
            AccountUserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!user.isEnabled() || !user.acceptsTokenIssuedAt(claims.getIssuedAt())) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("JWT rejeitado: {}", e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }
    }
}

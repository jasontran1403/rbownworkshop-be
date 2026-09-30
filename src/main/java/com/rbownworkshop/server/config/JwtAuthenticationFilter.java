package com.rbownworkshop.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbownworkshop.server.enumtype.StatusCode;
import com.rbownworkshop.server.repository.TokenRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final TokenRepository tokenRepository;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    if (isPublicPath(request.getServletPath())) {
      filterChain.doFilter(request, response);
      return;
    }

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendErrorResponse(response, StatusCode.UNAUTHORIZED, "Thiếu hoặc sai định dạng token");
      return;
    }

    final String jwt = authHeader.substring(7);

    try {
      final String userEmail = jwtService.extractUsername(jwt);

      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        boolean isTokenValidInDb = tokenRepository.findByToken(jwt)
                .map(t -> !t.isExpired() && !t.isRevoked())
                .orElse(false);

        if (jwtService.isTokenValid(jwt, userDetails) && isTokenValidInDb) {
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities()
          );
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);

          filterChain.doFilter(request, response);
          return;
        } else {
          log.warn("[Auth] Từ chối user='{}' {} {} — token không hợp lệ hoặc đã bị thu hồi",
                  userEmail, request.getMethod(), request.getRequestURI());
          sendErrorResponse(response, StatusCode.UNAUTHORIZED, "Token không hợp lệ hoặc đã bị thu hồi");
          return;
        }
      }

      sendErrorResponse(response, StatusCode.JWT_INVALID_SIGNATURE, "Token không hợp lệ (không extract được username)");
      return;
    } catch (ExpiredJwtException e) {
      log.warn("[Auth] Token hết hạn — user='{}', hết hạn lúc {}, request {} {}",
              safeSubject(e), safeExpiration(e), request.getMethod(), request.getRequestURI());
      sendErrorResponse(response, StatusCode.JWT_EXPIRED, "Phiên đăng nhập đã hết hạn");

    } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
      log.warn("[Auth] Token sai chữ ký/định dạng — request {} {}: {}",
              request.getMethod(), request.getRequestURI(), e.getMessage());
      sendErrorResponse(response, StatusCode.JWT_INVALID_SIGNATURE, "Token không hợp lệ (chữ ký hoặc định dạng sai)");

    } catch (Exception e) {
      log.error("Lỗi xử lý JWT không xác định: {}", e.getMessage(), e);
      sendErrorResponse(response, StatusCode.UNAUTHORIZED, "Lỗi xác thực token");
    }
  }

  private static String safeSubject(ExpiredJwtException e) {
    try {
      return e.getClaims() != null ? e.getClaims().getSubject() : "?";
    } catch (Exception ignored) {
      return "?";
    }
  }

  private static String safeExpiration(ExpiredJwtException e) {
    try {
      return e.getClaims() != null ? String.valueOf(e.getClaims().getExpiration()) : "?";
    } catch (Exception ignored) {
      return "?";
    }
  }

  private boolean isPublicPath(String servletPath) {
    if (servletPath == null) return false;
    for (String prefix : SecurityConfiguration.PUBLIC_API_PREFIXES) {
      if (servletPath.startsWith(prefix)) return true;
    }
    return false;
  }

  private void sendErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json;charset=UTF-8");

    Map<String, Object> body = new HashMap<>();
    body.put("code", code);
    body.put("success", StatusCode.isSuccess(code));
    body.put("message", message);
    body.put("timestamp", Instant.now().toString());

    objectMapper.writeValue(response.getWriter(), body);
    response.getWriter().flush();
  }
}
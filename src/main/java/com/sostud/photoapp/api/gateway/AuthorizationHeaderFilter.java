package com.sostud.photoapp.api.gateway;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

  @Autowired
  private Environment env;

  public AuthorizationHeaderFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return ((exchange, chain) -> {
      final ServerHttpRequest request = exchange.getRequest();
      if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
      }
      final String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
      final String jwt = authHeader.replace("Bearer", "");
      if (!isJwtValid(jwt)) {
        return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
      }
      return chain.filter(exchange);
    });
  }

  private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
    final ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(httpStatus);
    return response.setComplete();
  }

  private boolean isJwtValid(String jwt) {
    boolean returnValue = true;
    final String subject = Jwts.parser()
        .setSigningKey(env.getProperty("token.secret"))
        .parseClaimsJws(jwt)
        .getBody()
        .getSubject();
    if (subject == null || subject.isEmpty()) {
      returnValue = false;
    }
    return returnValue;
  }

  public static class Config {
    // Put configuration properties here
  }

}

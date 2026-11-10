package com.saas.mall.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${saas.jwt.secret}")
    private String secret;

    @Value("${saas.jwt.expire-hours}")
    private long expireHours;

    public String createToken(Long userId, Long tenantId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("tenantId", tenantId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /** 解析失败返回 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }
}

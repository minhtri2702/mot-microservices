//package com.mot.authservice.security;
//
//
//import java.util.Date;
//
//public class JwtUtil {
//
//    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//    private static final long EXPIRATION = 86400000; // 1 day
//
//    public static String generateToken(String username) {
//        return Jwts.builder()
//                .setSubject(username)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
//                .signWith(KEY)
//                .compact();
//    }
//
//    public static String validateToken(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(KEY)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//}
//

package com.fraudtriage.security;

import com.fraudtriage.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtAuthenticationFilter(JwtService jwt){this.jwt=jwt;}

    @Override
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)
            throws ServletException,IOException {
        String header=req.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ")) {
            try {
                Claims c=jwt.parse(header.substring(7));
                String role=c.get("role",String.class);
                var auth=new UsernamePasswordAuthenticationToken(c.getSubject(),null,
                        List.of(new SimpleGrantedAuthority("ROLE_"+role)));
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            } catch(Exception ignored) {}
        }
        chain.doFilter(req,res);
    }
}

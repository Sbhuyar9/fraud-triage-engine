package com.fraudtriage.controller;

import com.fraudtriage.dto.*;
import com.fraudtriage.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final JwtService jwt;
    public AuthController(JwtService jwt){this.jwt=jwt;}

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest req) {
        String role;
        if ("admin".equals(req.username()) && "admin123".equals(req.password())) role="ADMIN";
        else if ("analyst".equals(req.username()) && "analyst123".equals(req.password())) role="ANALYST";
        else return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new TokenResponse(jwt.generate(req.username(),role),role));
    }
}

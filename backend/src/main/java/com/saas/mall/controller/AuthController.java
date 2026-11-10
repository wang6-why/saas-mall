package com.saas.mall.controller;

import com.saas.mall.common.R;
import com.saas.mall.entity.Tenant;
import com.saas.mall.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return R.ok(authService.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/register")
    public R<Tenant> register(@RequestBody Map<String, String> body) {
        return R.ok(authService.register(body.get("name"), body.get("username"), body.get("password")));
    }
}

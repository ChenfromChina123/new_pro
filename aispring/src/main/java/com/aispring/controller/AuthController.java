package com.aispring.controller;

import com.aispring.dto.request.*;
import com.aispring.dto.response.ApiResponse;
import com.aispring.dto.response.AuthResponse;
import com.aispring.service.AuthService;
import com.aispring.util.RsaUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 对应Python: routers/auth.py
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final RsaUtil rsaUtil;
    
    /**
     * 发送注册验证码
     * Python端点: POST /api/register/email
     */
    @PostMapping("/register/send-code")
    public ResponseEntity<ApiResponse<Void>> sendRegisterCode(
            @Valid @RequestBody EmailRequest request) {
        authService.sendRegisterCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的邮箱", null));
    }
    
    /**
     * 用户注册
     * Python端点: POST /api/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }
    
    /**
     * 用户注册（支持RSA加密密码）
     */
    @PostMapping("/encrypted-register")
    public ResponseEntity<ApiResponse<AuthResponse>> encryptedRegister(
            @Valid @RequestBody EncryptedRegisterRequest request) {
        try {
            String password = request.getPassword();
            
            // 如果密码已加密，则先解密
            if (Boolean.TRUE.equals(request.getEncrypted())) {
                password = rsaUtil.decrypt(password);
            }
            
            // 创建临时的RegisterRequest用于注册
            RegisterRequest tempRequest = new RegisterRequest();
            tempRequest.setUsername(request.getUsername());
            tempRequest.setEmail(request.getEmail());
            tempRequest.setPassword(password);
            tempRequest.setCode(request.getCode());
            
            AuthResponse response = authService.register(tempRequest);
            return ResponseEntity.ok(ApiResponse.success("注册成功", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "注册失败：" + e.getMessage()));
        }
    }
    
    /**
     * 用户登录
     * Python端点: POST /api/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }
    
    /**
     * 用户登录（支持RSA加密密码）
     */
    @PostMapping("/encrypted-login")
    public ResponseEntity<ApiResponse<AuthResponse>> encryptedLogin(
            @Valid @RequestBody EncryptedLoginRequest request) {
        try {
            String password = request.getPassword();
            
            // 如果密码已加密，则先解密
            if (Boolean.TRUE.equals(request.getEncrypted())) {
                password = rsaUtil.decrypt(password);
            }
            
            // 创建临时的LoginRequest用于认证
            LoginRequest tempRequest = new LoginRequest();
            tempRequest.setEmail(request.getEmail());
            tempRequest.setPassword(password);
            
            AuthResponse response = authService.login(tempRequest);
            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "登录失败：" + e.getMessage()));
        }
    }
    
    /**
     * 发送忘记密码验证码
     * Python端点: POST /api/forgot-password/email
     */
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<ApiResponse<Void>> sendForgotPasswordCode(
            @Valid @RequestBody EmailRequest request) {
        authService.sendForgotPasswordCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的邮箱", null));
    }
    
    /**
     * 重置密码
     * Python端点: POST /api/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }
    
    /**
     * 测试接口：生成测试用户token
     * 仅用于开发测试环境
     */
    @PostMapping("/test/token")
    public ResponseEntity<ApiResponse<AuthResponse>> generateTestToken() {
        AuthResponse response = authService.createTestUser();
        return ResponseEntity.ok(ApiResponse.success("测试token生成成功", response));
    }
    
    /**
     * 验证token有效性
     * 用于前端检查token是否有效，实现持久化登录
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken() {
        // 如果能到达这里，说明token已经被JWT过滤器验证通过
        return ResponseEntity.ok(ApiResponse.success("Token有效", true));
    }
}


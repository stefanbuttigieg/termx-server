package com.kodality.termx.uam.auth.controller;

import com.kodality.termx.core.auth.Authorized;
import com.kodality.termx.uam.auth.service.AuthenticationService;
import com.kodality.termx.uam.auth.service.AuthenticationService.AuthenticationRequest;
import com.kodality.termx.uam.auth.service.AuthenticationService.AuthenticationResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.RequiredArgsConstructor;

@Controller("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    @Post("/login")
    public HttpResponse<AuthenticationResponse> login(@Body AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        return HttpResponse.ok(response);
    }
    
    @Post("/refresh")
    public HttpResponse<AuthenticationResponse> refreshToken(@Body RefreshTokenRequest request) {
        AuthenticationResponse response = authenticationService.refreshToken(request.refreshToken());
        return HttpResponse.ok(response);
    }
    
    @Authorized
    @Post("/logout")
    public HttpResponse<Void> logout(@Body RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
        return HttpResponse.ok();
    }
    
    public record RefreshTokenRequest(String refreshToken) {}
}

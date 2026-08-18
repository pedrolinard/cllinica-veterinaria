package com.vetclinic.api.auth;

import com.vetclinic.api.auth.dto.LoginRequest;
import com.vetclinic.api.auth.dto.LoginResponse;
import com.vetclinic.api.security.JwtService;
import com.vetclinic.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        // Delega ao AuthenticationManager (usa o DaoAuthenticationProvider configurado),
        // que lança BadCredentialsException em caso de email/senha incorretos.
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        var profile = new LoginResponse.UserProfile(
                principal.getId().toString(),
                principal.getName(),
                principal.getEmail(),
                principal.getRole()
        );

        return new LoginResponse(token, "Bearer", profile);
    }
}

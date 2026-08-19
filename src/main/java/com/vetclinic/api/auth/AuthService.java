package com.vetclinic.api.auth;

import com.vetclinic.api.auth.dto.LoginRequest;
import com.vetclinic.api.auth.dto.LoginResponse;
import com.vetclinic.api.security.JwtService;
import com.vetclinic.api.security.RevokedToken;
import com.vetclinic.api.security.RevokedTokenRepository;
import com.vetclinic.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest request) {
        String key = request.email().trim().toLowerCase();
        loginAttemptService.checkAllowed(key);

        try {
            // Delega ao AuthenticationManager (usa o DaoAuthenticationProvider configurado),
            // que lança BadCredentialsException em caso de email/senha incorretos.
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            loginAttemptService.registerSuccess(key);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(principal);

            var profile = new LoginResponse.UserProfile(
                    principal.getId().toString(),
                    principal.getName(),
                    principal.getEmail(),
                    principal.getRole()
            );

            return new LoginResponse(token, "Bearer", profile);
        } catch (BadCredentialsException ex) {
            loginAttemptService.registerFailure(key);
            throw ex;
        }
    }

    /**
     * Revoga o token informado antes que ele expire naturalmente, para que um
     * logout realmente encerre a sessão no servidor (e não só no cliente).
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        String jti = jwtService.extractJti(token);
        if (jti == null) {
            return;
        }

        revokedTokenRepository.save(new RevokedToken(jti, jwtService.extractExpiration(token)));
    }
}

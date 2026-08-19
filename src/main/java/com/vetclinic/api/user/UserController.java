package com.vetclinic.api.user;

import com.vetclinic.api.security.UserPrincipal;
import com.vetclinic.api.user.dto.ChangePasswordRequest;
import com.vetclinic.api.user.dto.CreateUserRequest;
import com.vetclinic.api.user.dto.UpdateUserRequest;
import com.vetclinic.api.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestão da equipe da clínica (funcionários). Restrito a ADMIN,
 * já que envolve criar contas com acesso ao sistema.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Gestão de funcionários da clínica (somente ADMIN)")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Cria um novo funcionário (admin, veterinário ou recepcionista)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lista todos os funcionários")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/vets")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Lista os veterinários (para seleção ao agendar consultas)")
    public ResponseEntity<List<UserResponse>> findVets() {
        return ResponseEntity.ok(userService.findVets());
    }

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Troca a própria senha (qualquer funcionário autenticado)")
    public ResponseEntity<Void> changeOwnPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changeOwnPassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário pelo id")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um funcionário")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um funcionário")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

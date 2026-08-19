package com.vetclinic.api.user;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.audit.AuditAction;
import com.vetclinic.api.audit.AuditService;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.InvalidCurrentPasswordException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
import com.vetclinic.api.user.dto.ChangePasswordRequest;
import com.vetclinic.api.user.dto.CreateUserRequest;
import com.vetclinic.api.user.dto.UpdateUserRequest;
import com.vetclinic.api.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Já existe um usuário com o email: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        User saved = userRepository.save(user);
        auditService.record("User", saved.getId(), AuditAction.CREATE,
                "Funcionário criado: " + saved.getEmail() + " (" + saved.getRole() + ")");
        return UserResponse.from(saved);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> findVets() {
        return userRepository.findByRoleOrderByNameAsc(Role.VET).stream().map(UserResponse::from).toList();
    }

    public UserResponse findById(UUID id) {
        return UserResponse.from(getOrThrow(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        Role previousRole = user.getRole();

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Já existe um usuário com o email: " + request.email());
            }
        });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());

        UserResponse response = UserResponse.from(userRepository.save(user));
        if (previousRole != request.role()) {
            auditService.record("User", id, AuditAction.UPDATE,
                    "Papel de " + user.getEmail() + " alterado de " + previousRole + " para " + request.role());
        }
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        User user = getOrThrow(id);

        long appointments = appointmentRepository.countByVetId(id);
        if (appointments > 0) {
            throw new ConflictException(
                    "Não é possível excluir: este funcionário possui " + appointments
                            + " consulta(s) vinculada(s) como veterinário."
            );
        }

        userRepository.delete(user);
        auditService.record("User", id, AuditAction.DELETE, "Funcionário removido: " + user.getEmail());
    }

    @Transactional
    public void changeOwnPassword(UUID id, ChangePasswordRequest request) {
        User user = getOrThrow(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Senha atual incorreta.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }
}

package com.vetclinic.api.user;

import com.vetclinic.api.appointment.AppointmentRepository;
import com.vetclinic.api.common.exception.ConflictException;
import com.vetclinic.api.common.exception.ResourceNotFoundException;
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

        return UserResponse.from(userRepository.save(user));
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

        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Já existe um usuário com o email: " + request.email());
            }
        });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Usuário", id);
        }

        long appointments = appointmentRepository.countByVetId(id);
        if (appointments > 0) {
            throw new ConflictException(
                    "Não é possível excluir: este funcionário possui " + appointments
                            + " consulta(s) vinculada(s) como veterinário."
            );
        }

        userRepository.deleteById(id);
    }

    public User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }
}

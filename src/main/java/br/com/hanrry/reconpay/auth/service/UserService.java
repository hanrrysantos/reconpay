package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.CreateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.mapper.IUserMapper;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository userRepository;
    private final IUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO register(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email já cadastrado: " + request.email());
        }

        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public UserResponseDTO createUser(CreateUserRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email já cadastrado: " + request.email());
        }

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable)
                .map(userMapper::toDTO);
    }

    public UserResponseDTO findById(UUID id) {
        UserEntity user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com id: " + id));
        return userMapper.toDTO(user);
    }

    public UserResponseDTO findByEmail(String email) {
        UserEntity user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com email: " + email));
        return userMapper.toDTO(user);
    }

    @Transactional
    public UserResponseDTO updateName(UUID id, UpdateUserRequestDTO request) {
        UserEntity user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com id: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public void deleteById(UUID id) {
        UserEntity user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com id: " + id));
        user.setActive(false);
        userRepository.save(user);
    }
}

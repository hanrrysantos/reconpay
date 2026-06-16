package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import br.com.hanrry.reconpay.auth.mapper.IUserMapper;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final IUserRepository userRepository;
    private final IUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO register(UserRequestDTO request){
        if(userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        UserEntity user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.password()));

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public List<UserResponseDTO> findAllUsers() {

        List<UserEntity> users = userRepository.findAll();

        return userMapper.toDTOList(users);
    }

    public UserResponseDTO findUserById(UUID id){
        UserEntity user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + id)
        );

        return userMapper.toDTO(user);
    }

    public UserResponseDTO findUserByEmail(String email){
        UserEntity  user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email)
        );

        return userMapper.toDTO(user);
    }

    public UserResponseDTO updateUserNameById(UUID id,  UpdateUserRequestDTO request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));

        if(request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }

        UserEntity savedUser = userRepository.save(user);

        return userMapper.toDTO(savedUser);
    }

    public void deleteUserById(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
    }
}

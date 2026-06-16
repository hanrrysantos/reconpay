package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.auth.exception.UserNotFoundException;
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

    private final IUserRepository IUserRepository;
    private final IUserMapper IUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO register(UserRequestDTO request){
        if(IUserRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        UserEntity user = IUserMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.password()));

        UserEntity savedUser = IUserRepository.save(user);

        return IUserMapper.toDTO(savedUser);
    }

    public UserResponseDTO findUserById(UUID id){
        UserEntity user = IUserRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + id)
        );

        return IUserMapper.toDTO(user);
    }

    public List<UserResponseDTO> findAllUsers() {

        List<UserEntity> users = IUserRepository.findAll();

        return IUserMapper.toDTOList(users);
    }


    public UserResponseDTO findUserByEmail(String email){
        UserEntity  user = IUserRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email)
        );

        return IUserMapper.toDTO(user);
    }

    public void deleteUser(UUID id) {
        UserEntity user = IUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        user.setActive(false);
        IUserRepository.save(user);
    }
}

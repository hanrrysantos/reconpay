package br.com.hanrry.reconpay.auth.mapper;

import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IUserMapper {

    UserResponseDTO toDTO(UserEntity entity);

    UserEntity toEntity(UserRequestDTO request);


    List<UserResponseDTO> toDTOList(List<UserEntity> users);
}

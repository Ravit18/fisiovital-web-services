package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.AuthenticatedUserResponseDTO;
import com.healthdev.fisiovital.dto.SignUpPatientRequestDTO;
import com.healthdev.fisiovital.dto.SignUpPhysiotherapistRequestDTO;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** US01-US03: RequestDTO de registro -> entidades de perfil, y usuario autenticado -> ResponseDTO. */
@Mapper(config = MapStructConfig.class)
public interface AuthMapper {

    @Mapping(target = "user", source = "user")
    Patient toPatient(SignUpPatientRequestDTO request, User user);

    @Mapping(target = "user", source = "user")
    Physiotherapist toPhysiotherapist(SignUpPhysiotherapistRequestDTO request, User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "profileId", source = "profileId")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "token", source = "token")
    AuthenticatedUserResponseDTO toResponse(User user, Long profileId, String fullName, String token);
}

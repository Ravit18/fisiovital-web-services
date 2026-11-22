package com.healthdev.fisiovital.mapper;

import com.healthdev.fisiovital.dto.request.SignUpPatientRequestDTO;
import com.healthdev.fisiovital.dto.request.SignUpPhysiotherapistRequestDTO;
import com.healthdev.fisiovital.dto.response.AuthenticatedUserResponseDTO;
import com.healthdev.fisiovital.model.Patient;
import com.healthdev.fisiovital.model.Physiotherapist;
import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.model.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-11-22T12:47:07-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Eclipse Adoptium)"
)
@Component
public class AuthMapperImpl implements AuthMapper {

    @Override
    public Patient toPatient(SignUpPatientRequestDTO request, User user) {
        if ( request == null && user == null ) {
            return null;
        }

        String fullName = null;
        String phone = null;
        if ( request != null ) {
            fullName = request.fullName();
            phone = request.phone();
        }
        User user1 = null;
        user1 = user;

        Patient patient = new Patient( user1, fullName, phone );

        return patient;
    }

    @Override
    public Physiotherapist toPhysiotherapist(SignUpPhysiotherapistRequestDTO request, User user) {
        if ( request == null && user == null ) {
            return null;
        }

        String fullName = null;
        String phone = null;
        String licenseNumber = null;
        Specialty specialty = null;
        int yearsOfExperience = 0;
        String description = null;
        if ( request != null ) {
            fullName = request.fullName();
            phone = request.phone();
            licenseNumber = request.licenseNumber();
            specialty = request.specialty();
            if ( request.yearsOfExperience() != null ) {
                yearsOfExperience = request.yearsOfExperience();
            }
            description = request.description();
        }
        User user1 = null;
        user1 = user;

        Physiotherapist physiotherapist = new Physiotherapist( user1, fullName, phone, licenseNumber, specialty, yearsOfExperience, description );

        return physiotherapist;
    }

    @Override
    public AuthenticatedUserResponseDTO toResponse(User user, Long profileId, String fullName, String token) {
        if ( user == null && profileId == null && fullName == null && token == null ) {
            return null;
        }

        Long userId = null;
        String email = null;
        String role = null;
        if ( user != null ) {
            userId = user.getId();
            email = user.getEmail();
            if ( user.getRole() != null ) {
                role = user.getRole().name();
            }
        }
        Long profileId1 = null;
        profileId1 = profileId;
        String fullName1 = null;
        fullName1 = fullName;
        String token1 = null;
        token1 = token;

        AuthenticatedUserResponseDTO authenticatedUserResponseDTO = new AuthenticatedUserResponseDTO( userId, profileId1, email, role, fullName1, token1 );

        return authenticatedUserResponseDTO;
    }
}

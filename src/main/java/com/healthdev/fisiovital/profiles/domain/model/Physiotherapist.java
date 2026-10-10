package com.healthdev.fisiovital.profiles.domain.model;

import com.healthdev.fisiovital.iam.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "physiotherapists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Physiotherapist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    /** Numero de colegiatura del Colegio de Tecnologos Medicos del Peru. */
    @Column(name = "license_number", nullable = false, unique = true, length = 10)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Specialty specialty;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active;

    public Physiotherapist(User user, String fullName, String phone, String licenseNumber,
                           Specialty specialty, int yearsOfExperience, String description) {
        this.user = user;
        this.fullName = fullName.trim();
        this.phone = phone;
        this.licenseNumber = licenseNumber.trim();
        this.specialty = specialty;
        this.yearsOfExperience = yearsOfExperience;
        this.description = description;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}

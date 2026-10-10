package com.healthdev.fisiovital.appointments.infrastructure.persistence;

import com.healthdev.fisiovital.treatment.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Consultas del historial de citas: sesiones ordenadas de la mas nueva a la mas antigua. */
@Repository
public interface AppointmentRepository extends JpaRepository<Session, Long> {

    String SELECT = "select s from Session s join fetch s.slot join fetch s.plan p " +
            "join fetch p.patient join fetch p.physiotherapist ";
    String NEWEST_FIRST = " order by s.slot.startTime desc";

    @Query(SELECT + "where p.patient.id = :patientId" + NEWEST_FIRST)
    List<Session> findByPatient(@Param("patientId") Long patientId);

    @Query(SELECT + "where p.physiotherapist.id = :physioId" + NEWEST_FIRST)
    List<Session> findByPhysiotherapist(@Param("physioId") Long physioId);

    @Query(SELECT + "where lower(p.patient.fullName) = lower(:fullName)" + NEWEST_FIRST)
    List<Session> findByPatientFullName(@Param("fullName") String fullName);
}

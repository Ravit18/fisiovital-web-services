package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * US14 - Historial paginado del paciente. Solo usa join fetch en asociaciones many-to-one,
     * por lo que la paginacion se aplica en base de datos y no carga todo el historial en memoria.
     */
    @Query(value = SELECT + "where p.patient.id = :patientId" + NEWEST_FIRST,
            countQuery = "select count(s) from Session s join s.plan p where p.patient.id = :patientId")
    Page<Session> findByPatient(@Param("patientId") Long patientId, Pageable pageable);

    /** US15 - Historial completo del fisioterapeuta con todos sus pacientes. */
    @Query(SELECT + "where p.physiotherapist.id = :physioId" + NEWEST_FIRST)
    List<Session> findByPhysiotherapist(@Param("physioId") Long physioId);

    /**
     * US16 - Historial de un paciente por su nombre completo (sin distinguir mayusculas), solo con el
     * fisioterapeuta que consulta: no debe ver las sesiones del paciente con otros profesionales.
     */
    @Query(SELECT + "where p.physiotherapist.id = :physioId and lower(p.patient.fullName) = lower(:fullName)"
            + NEWEST_FIRST)
    List<Session> findByPhysiotherapistAndPatientFullName(@Param("physioId") Long physioId,
                                                          @Param("fullName") String fullName);
}

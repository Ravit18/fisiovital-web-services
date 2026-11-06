-- FisioVital: historia clínica de demostración (PostgreSQL).
--
-- Agrega a los pacientes demo sesiones de las semanas pasadas (una por semana) con su nota de evolución, y una
-- nota general de antecedentes. Las sesiones futuras de cada plan se renumeran para que las pasadas sean 1, 2, 3…
--
-- Requisitos, en este orden:
--   1. Arrancar el backend al menos una vez con esta versión (crea la tabla clinical_notes y su columna session_id).
--   2. Cargar las cuentas y planes demo:  powershell -ExecutionPolicy Bypass -File scripts\seed-demo.ps1
--   3. Ejecutar este archivo sobre la base de la app, por ejemplo:
--        psql -h localhost -U postgres -d fisiovital -f scripts/seed-history.sql
--
-- Se puede ejecutar varias veces: cada plan que ya tiene su historia se salta; solo se agrega la que falta.
-- La API no permite crear horarios en el pasado; por eso estos datos se insertan directamente en la base.

CREATE OR REPLACE FUNCTION pg_temp.add_demo_history(p_patient text, p_physio text, p_diagnosis text,
                                                    p_general text, p_notes text[])
RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    v_plan_id    bigint;
    v_patient_id bigint;
    v_physio_id  bigint;
    v_total      int;
    v_used       int;
    v_count      int := array_length(p_notes, 1);
    v_hour       time;
    v_start      timestamp;
    v_slot_id    bigint;
    v_session_id bigint;
BEGIN
    SELECT t.id, t.patient_id, t.physiotherapist_id, t.total_sessions
      INTO v_plan_id, v_patient_id, v_physio_id, v_total
      FROM treatment_plans t
      JOIN patients p ON p.id = t.patient_id
      JOIN physiotherapists f ON f.id = t.physiotherapist_id
     WHERE p.full_name = p_patient AND f.full_name = p_physio AND t.diagnosis = p_diagnosis
     ORDER BY t.id
     LIMIT 1;

    IF v_plan_id IS NULL THEN
        RAISE NOTICE 'Sin plan "%" de % con %: corre primero seed-demo.ps1', p_diagnosis, p_patient, p_physio;
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM clinical_notes n JOIN sessions x ON x.id = n.session_id
                WHERE x.treatment_plan_id = v_plan_id) THEN
        RAISE NOTICE '%: el plan "%" ya tenía su historia, no se cambió', p_patient, p_diagnosis;
        RETURN;
    END IF;

    SELECT count(*) INTO v_used FROM sessions WHERE treatment_plan_id = v_plan_id;
    IF v_used + v_count > v_total THEN
        v_count := greatest(v_total - v_used, 0);
    END IF;

    -- Misma hora que sus citas futuras (o 09:00 si no tiene).
    SELECT coalesce(min(s.start_time::time), time '09:00') INTO v_hour
      FROM sessions x JOIN availability_slots s ON s.id = x.slot_id
     WHERE x.treatment_plan_id = v_plan_id;

    -- Las sesiones existentes pasan a ser las siguientes a las de la historia.
    UPDATE sessions SET session_number = session_number + v_count WHERE treatment_plan_id = v_plan_id;

    -- Antecedentes: nota general, escrita antes de la primera sesión.
    INSERT INTO clinical_notes (patient_id, physiotherapist_id, session_id, text, created_at)
    VALUES (v_patient_id, v_physio_id, NULL, p_general,
            current_date - (v_count * 7 + 1) + v_hour);

    FOR i IN 1..v_count LOOP
        v_start := current_date - ((v_count - i + 1) * 7) + v_hour;

        INSERT INTO availability_slots (physiotherapist_id, start_time, end_time, status, version)
        VALUES (v_physio_id, v_start, v_start + interval '1 hour', 'BOOKED', 0)
        RETURNING id INTO v_slot_id;

        INSERT INTO sessions (treatment_plan_id, slot_id, session_number, status, reminder_sent)
        VALUES (v_plan_id, v_slot_id, i, 'RESERVED', true)
        RETURNING id INTO v_session_id;

        INSERT INTO clinical_notes (patient_id, physiotherapist_id, session_id, text, created_at)
        VALUES (v_patient_id, v_physio_id, v_session_id, p_notes[i], v_start + interval '55 minutes');
    END LOOP;

    RAISE NOTICE '%: % sesiones pasadas con nota (%)', p_patient, v_count, p_diagnosis;
END;
$$;

DO $$
BEGIN
    PERFORM pg_temp.add_demo_history('Carlos Mendoza', 'Mariana Rojas', 'Esguince de tobillo grado II',
        'Antecedentes: torcedura del tobillo derecho jugando fútbol; llega con muletas y radiografía sin fractura.',
        ARRAY[
            'Dolor 7/10 al apoyar y edema moderado en el maléolo externo. Se aplica crioterapia y movilidad pasiva suave; se indica reposo relativo.',
            'Dolor 5/10. Disminuye el edema; tolera apoyo parcial sin muletas. Se agregan ejercicios isométricos de tobillo.',
            'Dolor 3/10. Camina sin cojera en distancias cortas. Inicia propiocepción en superficie estable; buena adherencia a los ejercicios en casa.'
        ]);

    PERFORM pg_temp.add_demo_history('Carlos Mendoza', 'Jorge Paredes', 'Cervicalgia postural',
        'El paciente cuenta con trabajo de oficina de 9 horas frente a la computadora; refiere dolor cervical desde hace 3 meses.',
        ARRAY[
            'Contractura en trapecio superior bilateral; rotación cervical limitada a la derecha. Terapia manual y ergonomía del puesto de trabajo.',
            'Mejora la rotación cervical. Refiere menos dolor al final de la jornada. Se pautan pausas activas cada hora.'
        ]);

    PERFORM pg_temp.add_demo_history('Ana Torres', 'Mariana Rojas', 'Tendinopatía rotuliana',
        'La paciente cuenta con ecografía que muestra engrosamiento del tendón rotuliano izquierdo; practica vóley 3 veces por semana.',
        ARRAY[
            'Dolor 6/10 al bajar escaleras y al saltar. Se suspende el salto y se inicia carga isométrica de cuádriceps.',
            'Dolor 4/10. Tolera sentadilla parcial sin dolor. Progresa a ejercicio excéntrico en plano declinado.'
        ]);

    PERFORM pg_temp.add_demo_history('Luis Quispe', 'Jorge Paredes', 'Lumbalgia mecánica crónica',
        'Antecedentes: lumbalgia de 2 años; trabaja como chofer. Sin signos de alarma neurológica.',
        ARRAY[
            'Dolor lumbar 7/10 al estar sentado más de una hora. Se trabaja control motor y respiración diafragmática.',
            'Dolor 6/10. Mejora la flexión del tronco. Se agrega puente de glúteo y caminata diaria de 20 minutos.',
            'Dolor 4/10. Ya conduce 2 horas con pausas sin aumento del dolor. Se progresa a fortalecimiento con banda elástica.'
        ]);

    PERFORM pg_temp.add_demo_history('Sofía Huamán', 'Lucía Vargas', 'Rehabilitación tras ACV isquémico',
        'La paciente cuenta con informe neurológico: hemiparesia derecha leve tras ACV isquémico hace 2 meses. Acude con su hija.',
        ARRAY[
            'Marcha con bastón y apoyo de la hija; fuerza 3/5 en miembro superior derecho. Se trabajan transferencias de la cama a la silla.',
            'Mejora el equilibrio en bipedestación (aguanta 30 segundos). Se agregan ejercicios de motricidad fina con plastilina.',
            'Camina 10 metros con bastón sin ayuda. Logra abotonarse la camisa con dificultad leve.',
            'Fuerza 4/5 en miembro superior derecho. Sube 5 escalones con pasamanos. La familia refiere más independencia en casa.'
        ]);

    PERFORM pg_temp.add_demo_history('Ana Torres', 'Diego Salazar', 'Rehabilitación tras cirugía de manguito rotador',
        'Antecedentes: reparación artroscópica del manguito rotador derecho hace 5 semanas. Usó cabestrillo 4 semanas.',
        ARRAY[
            'Elevación del hombro activa-asistida hasta 90°. Dolor 5/10 nocturno. Se inician péndulos y movilidad pasiva.',
            'Elevación hasta 120°. Ya duerme sin despertarse por dolor. Se agregan isométricos de rotadores.'
        ]);

    PERFORM pg_temp.add_demo_history('Luis Quispe', 'Diego Salazar', 'Recuperación tras reconstrucción de LCA',
        'El paciente cuenta con informe quirúrgico: reconstrucción de LCA de rodilla izquierda con injerto de isquiotibiales.',
        ARRAY[
            'Extensión completa de rodilla; flexión 95°. Marcha con una muleta. Se trabaja activación de cuádriceps.',
            'Flexión 115°, camina sin muletas. Inicia bicicleta estática sin resistencia.'
        ]);

    PERFORM pg_temp.add_demo_history('Sofía Huamán', 'Jorge Paredes', 'Dorsalgia por mala postura',
        'Dolor dorsal por pasar muchas horas sentada en silla de ruedas durante la primera etapa tras el ACV.',
        ARRAY[
            'Tensión en musculatura dorsal y escápulas adelantadas. Se corrige postura sentada y se enseña respiración costal.'
        ]);
END;
$$;

package com.healthdev.fisiovital.treatment.domain.model;

import java.util.List;

public enum SessionStatus {
    RESERVED,
    COMPLETED,
    CANCELLED,
    LATE_CANCELLED;

    /** Estados que cuentan como sesion vigente del plan. */
    public static final List<SessionStatus> ACTIVE = List.of(RESERVED, COMPLETED);
    public static final List<SessionStatus> CANCELLED_STATES = List.of(CANCELLED, LATE_CANCELLED);
}

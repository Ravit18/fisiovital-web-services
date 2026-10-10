package com.healthdev.fisiovital.clinical.interfaces.rest.resources;

import java.util.List;

/** diagnoses: diagnosticos iniciales de los planes; notes: de la mas reciente a la mas antigua (US15). */
public record ClinicalHistoryResource(Long patientId, String patientName, List<String> diagnoses,
                                      List<ClinicalNoteResource> notes, String message) {
}

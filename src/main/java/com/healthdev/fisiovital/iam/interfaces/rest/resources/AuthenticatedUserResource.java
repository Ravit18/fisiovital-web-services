package com.healthdev.fisiovital.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long userId, Long profileId, String email, String role,
                                        String fullName, String token) {
}

package com.fraudtriage.dto;

import jakarta.validation.constraints.NotBlank;

public record OverrideRequest(@NotBlank String status, @NotBlank String reason) {}

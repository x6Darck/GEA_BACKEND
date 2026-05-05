package com.calendario.callapp.callapp_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MicrosoftAuthRequest {

    @NotBlank
    private String idToken;
}

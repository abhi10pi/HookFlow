package Backend.HookFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEndpointRequest(
        @NotBlank @Size(max = 2048) 
        String url
    ) {
}
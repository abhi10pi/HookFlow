package Backend.HookFlow.dto;

import jakarta.validation.constraints.Size;

public record UpdateEndpointRequest(
        @Size(max = 2048) 
        String url,
        
        Boolean active
    ) {
}
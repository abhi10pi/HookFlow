package Backend.HookFlow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")@Email 
        String email,

        @NotBlank(message = "Password is required") 
        @Size(min = 8, max = 72, message = "must be 8-72 characters") 
        String password,

        @NotBlank(message = "Tenant Name is required")
        @Size(max = 100) 
        String tenantName
    ) {
}

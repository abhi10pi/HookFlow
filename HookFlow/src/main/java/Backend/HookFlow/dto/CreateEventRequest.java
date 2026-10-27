package Backend.HookFlow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record CreateEventRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9._-]{0,99}$",
                 message = "must be lowercase letters, digits, dots, dashes or underscores (max 100 characters)")
        String type,

        @NotNull Map<String, Object> payload) {
}
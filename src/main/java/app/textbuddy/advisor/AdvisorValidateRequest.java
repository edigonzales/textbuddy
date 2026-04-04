package app.textbuddy.advisor;

import java.util.List;

public record AdvisorValidateRequest(
        String text,
        List<String> docs
) {
}

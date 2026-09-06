package app.textbuddy.advisor;

import java.util.List;

public record AdvisorFixRequest(String text, List<Finding> findings) {

    public record Finding(String documentName, String ruleId, int start, int end, String suggestion) {
    }
}

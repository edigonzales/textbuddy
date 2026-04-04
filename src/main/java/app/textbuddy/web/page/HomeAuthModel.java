package app.textbuddy.web.page;

public record HomeAuthModel(
        boolean enabled,
        boolean authenticated,
        String statusTitle,
        String statusMessage,
        String userDisplayName,
        String loginUrl
) {
}

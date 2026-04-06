package app.textbuddy.quickaction;

public record MediumCurrentUser(
        String givenName,
        String familyName,
        String email
) {

    private static final String DEFAULT_GIVEN_NAME = "Vorname";
    private static final String DEFAULT_FAMILY_NAME = "Nachname";
    private static final String DEFAULT_EMAIL = "vorname.nachname@example.org";

    public MediumCurrentUser {
        givenName = fallback(givenName, DEFAULT_GIVEN_NAME);
        familyName = fallback(familyName, DEFAULT_FAMILY_NAME);
        email = fallback(email, DEFAULT_EMAIL);
    }

    public static MediumCurrentUser placeholder() {
        return new MediumCurrentUser(null, null, null);
    }

    public String fullName() {
        return (givenName + " " + familyName).trim();
    }

    private static String fallback(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}

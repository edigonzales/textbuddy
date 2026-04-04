const AUTH_REQUIRED_MESSAGE = "Anmeldung erforderlich.";
const ACCESS_DENIED_MESSAGE = "Zugriff verweigert.";

export async function extractErrorMessage(
  response: Response,
  fallbackMessage: string,
): Promise<string> {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("json")) {
    const payload = (await response.json()) as Record<string, unknown>;
    const detail = payload.detail;
    const message = payload.message;
    const title = payload.title;

    if (typeof detail === "string" && detail.trim().length > 0) {
      return detail;
    }

    if (typeof message === "string" && message.trim().length > 0) {
      return message;
    }

    if (typeof title === "string" && title.trim().length > 0) {
      return title;
    }
  }

  const text = (await response.text()).trim();

  if (text.length > 0) {
    return text;
  }

  if (response.status === 401) {
    return AUTH_REQUIRED_MESSAGE;
  }

  if (response.status === 403) {
    return ACCESS_DENIED_MESSAGE;
  }

  return fallbackMessage;
}

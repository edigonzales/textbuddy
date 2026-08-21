export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  const root = document.getElementById("editor-island-root");
  const csrfToken = root?.dataset.csrfToken ?? "";
  const csrfHeader = root?.dataset.csrfHeader ?? "";

  if (csrfToken && csrfHeader) {
    headers.set(csrfHeader, csrfToken);
  }

  return fetch(input, { ...init, headers });
}

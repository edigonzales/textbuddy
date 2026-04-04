export interface EditorAuthState {
  enabled: boolean;
  authenticated: boolean;
  loginUrl: string;
}

function readBoolean(value: string | undefined): boolean {
  return value === "true";
}

export function readEditorAuthState(root: HTMLElement): EditorAuthState {
  return {
    enabled: readBoolean(root.dataset.authEnabled),
    authenticated: readBoolean(root.dataset.authenticated),
    loginUrl: root.dataset.authLoginUrl ?? "",
  };
}

export function isApiLocked(root: HTMLElement): boolean {
  const auth = readEditorAuthState(root);
  return auth.enabled && !auth.authenticated;
}

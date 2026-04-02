import "./style.css";

const app = document.querySelector<HTMLDivElement>("#app");

if (app) {
  app.innerHTML = `
    <main class="workspace-preview">
      <span class="label">Vite + TypeScript Workspace</span>
      <h1>Textbuddy Editor Island</h1>
      <p>Dieser Bereich ist nur der vorbereitete Frontend-Arbeitsplatz fuer Slice 01.</p>
      <div class="card">
        <strong>Noch keine Tiptap-Logik</strong>
        <p>Die Insel wird erst im naechsten Slice funktional an die Shell angebunden.</p>
      </div>
    </main>
  `;
}

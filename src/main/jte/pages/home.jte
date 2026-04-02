@import app.textbuddy.web.page.HomePageModel
@param HomePageModel page

@template.layout.main(title = page.title(), content = @`
<div class="page-shell">
    <header class="app-header panel" data-testid="shell-header">
        <div>
            <p class="eyebrow">Spring MVC Shell</p>
            <h1>Textbuddy</h1>
            <p>${page.subtitle()}</p>
        </div>
        <div class="status-strip">
            <div class="status-pill">HTMX ist eingebunden, aber in Slice 00 noch ohne fachliche Nutzung.</div>
            <div class="status-pill">Das künftige Editor-Frontend lebt vorbereitet unter <code>frontend/</code>.</div>
        </div>
    </header>

    <main class="workspace-grid">
        <section class="panel" data-testid="editor-placeholder">
            <div class="panel-body">
                <p class="eyebrow">Arbeitsfläche</p>
                <h2>Editor-Container-Platzhalter</h2>
                <p class="panel-copy">Slice 01 hängt die Tiptap-Insel an diese sichtbare, derzeit leere Arbeitsoberfläche.</p>

                <div id="editor-island-root" class="editor-root" aria-disabled="true">
                    <div class="chip-row">
                        <span class="chip">JTE rendert die Shell</span>
                        <span class="chip">Spring MVC liefert die Seite</span>
                        <span class="chip">Keine Editorlogik in Slice 00</span>
                    </div>

                    <div class="editor-canvas">
                        <strong>Leere Arbeitsoberfläche</strong>
                        <p>Hier erscheint später die Tiptap-Insel. Aktuell bleibt der Bereich absichtlich ohne fachliche Interaktion.</p>
                    </div>
                </div>

                <div class="tool-grid" data-testid="tools-placeholder">
                    <button type="button" disabled>Textkorrektur</button>
                    <button type="button" disabled>Rewrite</button>
                    <button type="button" disabled>Synonyme</button>
                    <button type="button" disabled>Quick Actions</button>
                    <button type="button" disabled>Advisor</button>
                    <button type="button" disabled>Dokumentimport</button>
                </div>
            </div>
        </section>

        <aside class="panel" data-testid="panel-placeholder">
            <div class="panel-body">
                <p class="eyebrow">Rechtes Panel</p>
                <h2>Seitenpanel-Platzhalter</h2>
                <p class="panel-copy">Dieser Bereich bleibt in Slice 00 rein visuell und markiert die spätere Fläche für serverseitige Fragmente und Tool-Panels.</p>

                <div class="stack-list">
                    <div class="stack-item">
                        <strong>Spätere Fragmente</strong>
                        Advisor-Listen, Rewrite-Optionen und Tool-Ausgaben werden erst in späteren Slices ergänzt.
                    </div>
                    <div class="stack-item">
                        <strong>Keine API-Vorgriffe</strong>
                        Es existiert bewusst noch kein Endpoint außer <code>GET /</code>.
                    </div>
                    <div class="stack-item">
                        <strong>Stub-Basis aktiv</strong>
                        Kernservices und Adapter sind nur als compilebare Beans und Interfaces vorhanden.
                    </div>
                </div>
            </div>
        </aside>
    </main>
</div>
`)

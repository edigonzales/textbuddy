@import app.textbuddy.web.page.HomePageModel
@param HomePageModel page

@template.layout.main(title = page.title(), content = @`
<div class="page-shell">
    <header class="app-header panel" data-testid="shell-header">
        <div>
            <p class="eyebrow">Spring MVC + Tiptap Island</p>
            <h1>Textbuddy</h1>
            <p>${page.subtitle()}</p>
        </div>
        <div class="status-strip">
            <div class="status-pill">Slice 01 bindet die Editor-Insel direkt in die serverseitige JTE-Shell ein.</div>
            <div class="status-pill">Mirror, Zähler und Undo/Redo laufen rein clientseitig ohne Fach-API-Aufruf.</div>
        </div>
    </header>

    <main class="workspace-grid">
        <section class="panel" data-testid="editor-shell">
            <div class="panel-body">
                <p class="eyebrow">Arbeitsfläche</p>
                <h2>Editor Island Base</h2>
                <p class="panel-copy">Der Editor bearbeitet aktuell nur Plain Text und hält Mirror, Counter sowie Undo/Redo lokal synchron.</p>

                <div id="editor-island-root" class="editor-root" data-testid="editor-island-root">
                    <div class="chip-row">
                        <span class="chip">JTE rendert die Shell</span>
                        <span class="chip">Tiptap bearbeitet Plain Text</span>
                        <span class="chip">Keine Server-API in Slice 01</span>
                    </div>

                    <div class="editor-toolbar" data-testid="editor-toolbar">
                        <div class="editor-toolbar-group">
                            <button type="button" class="editor-button" data-editor-action="undo" data-testid="editor-undo" disabled>Undo</button>
                            <button type="button" class="editor-button" data-editor-action="redo" data-testid="editor-redo" disabled>Redo</button>
                        </div>

                        <div class="editor-counter-bar" aria-live="polite">
                            <span class="counter-pill"><strong data-editor-count="characters" data-testid="editor-character-count">0</strong> Zeichen</span>
                            <span class="counter-pill"><strong data-editor-count="words" data-testid="editor-word-count">0</strong> Wörter</span>
                        </div>
                    </div>

                    <div class="editor-canvas" data-testid="editor-canvas">
                        <div class="editor-surface" data-editor-surface data-editor-placeholder="Schreibe hier deinen Text."></div>
                        <textarea hidden name="text" data-editor-mirror data-testid="editor-mirror"></textarea>
                    </div>
                </div>
            </div>
        </section>

        <aside class="panel" data-testid="panel-placeholder">
            <div class="panel-body">
                <p class="eyebrow">Slice-Status</p>
                <h2>Lokale Editorbasis</h2>
                <p class="panel-copy">Die rechte Seite bleibt bewusst schlicht und dokumentiert nur den aktuell freigeschalteten Umfang dieses Slices.</p>

                <div class="stack-list">
                    <div class="stack-item">
                        <strong>Clientseitige Events</strong>
                        Die Insel feuert <code>editor:text-changed</code> und <code>editor:selection-changed</code> fuer spaetere Integrationen.
                    </div>
                    <div class="stack-item">
                        <strong>Keine API-Vorgriffe</strong>
                        Auch in Slice 01 bleibt es bewusst bei <code>GET /</code>; Textverarbeitung folgt erst spaeter.
                    </div>
                    <div class="stack-item">
                        <strong>Naechster Andockpunkt</strong>
                        Slice 02 kann denselben Mirror-Text erstmals in die Textkorrektur-API einspeisen.
                    </div>
                </div>
            </div>
        </aside>
    </main>
</div>
`)

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
            <div class="status-pill">Slice 03 prueft nur geaenderte Segmente ueber <code>POST /api/text-correction</code> und triggert sofort bei Punkt oder Zeilenumbruch.</div>
            <div class="status-pill">Sprache, lokales Woerterbuch, Markierungen und Suggestion-Apply bleiben vollstaendig in der JS-Insel und ziehen keine spaeteren Features vor.</div>
        </div>
    </header>

    <main class="workspace-grid">
        <section class="panel" data-testid="editor-shell">
            <div class="panel-body">
                <p class="eyebrow">Arbeitsfläche</p>
                <h2>Text Correction Enhancements</h2>
                <p class="panel-copy">Die Korrektur prueft nur geaenderte Segmente, reagiert sofort auf Satzgrenzen und laesst bekannte Woerter lokal im Browser ausblenden.</p>

                <div id="editor-island-root" class="editor-root" data-testid="editor-island-root">
                    <div class="chip-row">
                        <span class="chip">JTE rendert die Shell</span>
                        <span class="chip">Tiptap bearbeitet Plain Text</span>
                        <span class="chip">LanguageTool prueft nur geaenderte Segmente</span>
                        <span class="chip">Punkt und Zeilenumbruch triggern sofort</span>
                        <span class="chip">Vorschlaege werden im Editor angewendet</span>
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

        <aside class="panel correction-panel" data-testid="correction-panel" data-correction-panel data-correction-state="idle">
            <div class="panel-body">
                <p class="eyebrow">Problems Panel</p>
                <h2>Textkorrektur</h2>
                <p class="panel-copy">Die Insel steuert Sprache, lokales Woerterbuch und differenzierte Re-Checks. Der Server bleibt beim bestehenden Korrektur-Endpoint.</p>

                <div class="correction-controls" data-testid="correction-controls">
                    <label class="control-stack" for="correction-language">
                        <span class="control-label">Korrektursprache</span>
                        <select id="correction-language" class="control-select" data-correction-language data-testid="correction-language">
                            <option value="auto" selected>Automatisch</option>
                            <option value="de-DE">Deutsch</option>
                            <option value="en-US">Englisch</option>
                        </select>
                    </label>

                    <div class="dictionary-shell" data-testid="dictionary-shell">
                        <div class="dictionary-head">
                            <strong>Lokales Woerterbuch</strong>
                            <span>Nur browserlokal, ohne Serverpersistenz</span>
                        </div>

                        <form class="dictionary-form" data-dictionary-form data-testid="dictionary-form">
                            <input type="text" class="dictionary-input" data-dictionary-input data-testid="dictionary-input" placeholder="Wort hinzufuegen" autocomplete="off" />
                            <button type="submit" class="dictionary-submit" data-testid="dictionary-submit">Hinzufuegen</button>
                        </form>

                        <p class="dictionary-empty" data-dictionary-empty data-testid="dictionary-empty">Noch keine lokalen Woerter gespeichert.</p>
                        <ul class="dictionary-list" data-dictionary-list data-testid="dictionary-list"></ul>
                    </div>
                </div>

                <p class="problem-status" data-correction-status data-testid="correction-status">Schreibe Text, um Korrekturen zu sehen.</p>
                <div class="problem-list" data-correction-list data-testid="correction-problem-list"></div>

                <div class="stack-list problems-footnotes">
                    <div class="stack-item">
                        <strong>Bewusst noch nicht aktiv</strong>
                        Kein Satz-Rewrite, keine Synonyme, keine Quick Actions und keine spaeteren Bubble-Menues in Slice 03.
                    </div>
                    <div class="stack-item">
                        <strong>Editorinteraktion bleibt clientseitig</strong>
                        Die serverseitige API liefert weiter nur Korrekturblocke; Markierung, Fokus, Woerterbuchfilter und diff-basierte Re-Checks laufen in der Insel.
                    </div>
                </div>
            </div>
        </aside>
    </main>
</div>
`)

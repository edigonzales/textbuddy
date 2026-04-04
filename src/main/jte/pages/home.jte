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
            <div class="status-pill" data-testid="release-status-pill">
                Slice 18 kombiniert die vorhandenen Kernfeatures jetzt mit konfigurierbarer OIDC-Grundintegration, globalem Fehlerbild und einem produktionsnaeheren Startprofil.
            </div>
            <div class="status-pill" data-testid="auth-status-pill">
                <strong>${page.auth().statusTitle()}</strong>
                <span>${page.auth().statusMessage()}</span>
            </div>
            @if (page.auth().enabled() && page.auth().authenticated())
                <div class="status-pill status-pill-account" data-testid="auth-account-pill">
                    <strong data-testid="auth-user">${page.auth().userDisplayName()}</strong>
                    <form method="post" action="/logout" class="auth-form" data-testid="auth-logout-form">
                        <button type="submit" class="inline-action" data-testid="auth-logout">Abmelden</button>
                    </form>
                </div>
            @elseif (page.auth().enabled())
                <div class="status-pill status-pill-account" data-testid="auth-login-pill">
                    <strong>Anmeldung</strong>
                    <a class="inline-action" href="${page.auth().loginUrl()}" data-testid="auth-login-link">Mit OIDC anmelden</a>
                </div>
            @endif
        </div>
    </header>

    <main class="workspace-grid">
        <section class="panel" data-testid="editor-shell">
            <div class="panel-body">
                <p class="eyebrow">Arbeitsfläche</p>
                <h2>Quick Actions + Rewrite Bubble</h2>
                <p class="panel-copy">Die Insel kombiniert Wort-/Satz-Rewrite im Bubble-Menue, alle neun streamingfaehigen Volltext-Rewrites, Dokumentimport, Advisor-Pruefung und Auth-/Fehlerpolitur in einer gemeinsamen Arbeitsflaeche.</p>

                <div
                    id="editor-island-root"
                    class="editor-root"
                    data-testid="editor-island-root"
                    data-auth-enabled="${Boolean.toString(page.auth().enabled())}"
                    data-authenticated="${Boolean.toString(page.auth().authenticated())}"
                    data-auth-login-url="${page.auth().loginUrl()}"
                >
                    <div class="chip-row">
                        <span class="chip">JTE rendert die Shell</span>
                        <span class="chip">Tiptap bearbeitet Plain Text</span>
                        <span class="chip">LanguageTool prueft nur geaenderte Segmente</span>
                        <span class="chip">Fokussiertes Wort hat Vorrang vor Satzfokus</span>
                        <span class="chip">Dasselbe Bubble-Menue startet Wort-Synonyme oder Satzumformung</span>
                        <span class="chip">Plain Language streamt ueber SSE direkt in den Editor</span>
                        <span class="chip">Bullet Points nutzt dieselbe Streaming- und Diff-Infrastruktur</span>
                        <span class="chip">Proofread nutzt dieselbe Streaming- und Diff-Infrastruktur</span>
                        <span class="chip">Summarize waehlt seine Variante ueber ein eigenes Dropdown</span>
                        <span class="chip">Formality waehlt zwischen formal und informal</span>
                        <span class="chip">Social Media waehlt zwischen Bluesky, Instagram und LinkedIn</span>
                        <span class="chip">Medium waehlt zwischen E-Mail, offiziellem Schreiben, Praesentation und Bericht</span>
                        <span class="chip">Character Speech waehlt zwischen direkter und indirekter Rede</span>
                        <span class="chip">Custom nimmt einen freien Arbeitsauftrag im Rewrite-Bereich entgegen</span>
                        <span class="chip">Rewrite-Diff und kompletter Undo bauen auf derselben Streaming-Basis auf</span>
                        <span class="chip">Dokumentimport gleicht Upload-Formate zwischen UI und Backend ab</span>
                        <span class="chip">API-Fehler liefern ein konsistentes Problem-JSON mit Trace-ID</span>
                        <span class="chip">OIDC bleibt per Konfiguration aktivierbar, ohne den lokalen Start zu blockieren</span>
                    </div>

                    <section
                        class="document-import-panel"
                        data-document-import-panel
                        data-document-import-state="idle"
                        data-document-import-labels="${String.join(", ", page.documentImportFormats())}"
                        data-testid="document-import-panel"
                    >
                        <div class="document-import-head">
                            <div>
                                <p class="eyebrow">Document Import</p>
                                <h3>Upload oder Drag-and-Drop</h3>
                                <p class="panel-copy">Unterstuetzte Dokumente werden an Docling gesendet. Das zurueckgelieferte HTML landet direkt in der bestehenden Tiptap-Insel, ohne Upload-Persistenz ausserhalb des aktuellen Requests. Bei aktiver OIDC-Konfiguration laeuft der Endpoint nur fuer angemeldete Sessions.</p>
                            </div>

                            <p class="document-import-status" data-document-import-status data-testid="document-import-status" aria-live="polite">Bereit fuer Upload oder Drag-and-Drop.</p>
                        </div>

                        <div class="document-import-body">
                            <div
                                class="document-import-dropzone"
                                data-document-import-dropzone
                                data-dragging="false"
                                data-busy="false"
                                data-testid="document-import-dropzone"
                                role="button"
                                tabindex="0"
                                aria-controls="document-import-input"
                            >
                                <input
                                    id="document-import-input"
                                    class="document-import-input"
                                    data-document-import-input
                                    data-testid="document-import-input"
                                    type="file"
                                    accept="${page.documentImportAccept()}"
                                >
                                <strong>Dokument hier ablegen</strong>
                                <span>Ziehe eine Datei auf diese Flaeche oder waehle sie ueber den Upload-Button aus.</span>
                                <div class="document-import-format-list" data-testid="document-import-format-list">
                                    @for (var format : page.documentImportFormats())
                                        <span class="document-import-format">${format}</span>
                                    @endfor
                                </div>
                            </div>

                            <button type="button" class="editor-button document-import-button" data-document-import-button data-testid="document-import-button">Upload starten</button>
                        </div>
                    </section>

                    <section class="quick-action-panel" data-quick-action-panel data-testid="quick-action-panel" data-quick-action-state="idle">
                        <div class="quick-action-panel-head">
                            <div>
                                <p class="eyebrow">Quick Actions</p>
                                <h3>Rewrite mit Streaming</h3>
                                <p class="panel-copy">Plain Language, Bullet Points, Proofread, Summarize, Formality, Social Media, Medium, Character Speech und Custom bleiben aktiv. Summarize, Formality, Social Media, Medium und Character Speech nutzen eigene Varianten-Menues, Custom nutzt ein freies Prompt-Feld, weitere Quick Actions werden bewusst nicht nachgezogen.</p>
                            </div>

                            <p class="quick-action-status" data-quick-action-status data-testid="quick-action-status" aria-live="polite">Bereit fuer Plain Language, Bullet Points, Proofread, Summarize, Formality, Social Media, Medium, Character Speech und Custom.</p>
                        </div>

                        <div class="quick-action-row">
                            <button type="button" class="quick-action-button" data-quick-action="plain-language" data-testid="quick-action-plain-language">Plain Language</button>
                            <button type="button" class="quick-action-button" data-quick-action="bullet-points" data-testid="quick-action-bullet-points">Bullet Points</button>
                            <button type="button" class="quick-action-button" data-quick-action="proofread" data-testid="quick-action-proofread">Proofread</button>
                            <div class="quick-action-menu" data-testid="quick-action-summarize-menu">
                                <label class="quick-action-select-shell" for="quick-action-summarize-option">
                                    <span class="quick-action-select-label">Summarize-Variante</span>
                                    <select id="quick-action-summarize-option" class="quick-action-select" data-quick-action-option="summarize" data-testid="quick-action-summarize-option">
                                        <option value="sentence" selected>Ein Satz</option>
                                        <option value="three_sentence">Drei Saetze</option>
                                        <option value="paragraph">Ein Absatz</option>
                                        <option value="page">Eine Seite</option>
                                        <option value="management_summary">Management Summary</option>
                                    </select>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="summarize" data-testid="quick-action-summarize">Summarize</button>
                            </div>
                            <div class="quick-action-menu" data-testid="quick-action-formality-menu">
                                <label class="quick-action-select-shell" for="quick-action-formality-option">
                                    <span class="quick-action-select-label">Formality-Variante</span>
                                    <select id="quick-action-formality-option" class="quick-action-select" data-quick-action-option="formality" data-testid="quick-action-formality-option">
                                        <option value="formal" selected>Formell</option>
                                        <option value="informal">Informell</option>
                                    </select>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="formality" data-testid="quick-action-formality">Formality</button>
                            </div>
                            <div class="quick-action-menu" data-testid="quick-action-social-media-menu">
                                <label class="quick-action-select-shell" for="quick-action-social-media-option">
                                    <span class="quick-action-select-label">Social-Media-Kanal</span>
                                    <select id="quick-action-social-media-option" class="quick-action-select" data-quick-action-option="social-media" data-testid="quick-action-social-media-option">
                                        <option value="bluesky" selected>Bluesky</option>
                                        <option value="instagram">Instagram</option>
                                        <option value="linkedin">LinkedIn</option>
                                    </select>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="social-media" data-testid="quick-action-social-media">Social Media</button>
                            </div>
                            <div class="quick-action-menu" data-testid="quick-action-medium-menu">
                                <label class="quick-action-select-shell" for="quick-action-medium-option">
                                    <span class="quick-action-select-label">Medium-Typ</span>
                                    <select id="quick-action-medium-option" class="quick-action-select" data-quick-action-option="medium" data-testid="quick-action-medium-option">
                                        <option value="email" selected>E-Mail</option>
                                        <option value="official_letter">Offizieller Brief</option>
                                        <option value="presentation">Praesentation</option>
                                        <option value="report">Bericht</option>
                                    </select>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="medium" data-testid="quick-action-medium">Medium</button>
                            </div>
                            <div class="quick-action-menu" data-testid="quick-action-character-speech-menu">
                                <label class="quick-action-select-shell" for="quick-action-character-speech-option">
                                    <span class="quick-action-select-label">Redeform</span>
                                    <select id="quick-action-character-speech-option" class="quick-action-select" data-quick-action-option="character-speech" data-testid="quick-action-character-speech-option">
                                        <option value="direct_speech" selected>Direkte Rede</option>
                                        <option value="indirect_speech">Indirekte Rede</option>
                                    </select>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="character-speech" data-testid="quick-action-character-speech">Character Speech</button>
                            </div>
                            <div class="quick-action-menu quick-action-menu-wide" data-testid="quick-action-custom-menu">
                                <label class="quick-action-select-shell" for="quick-action-custom-prompt">
                                    <span class="quick-action-select-label">Custom-Auftrag</span>
                                    <textarea id="quick-action-custom-prompt" class="quick-action-textarea" data-quick-action-prompt="custom" data-testid="quick-action-custom-prompt" rows="3" maxlength="400" placeholder="Beschreibe frei, wie der Volltext umgeschrieben werden soll."></textarea>
                                </label>
                                <button type="button" class="quick-action-button" data-quick-action="custom" data-testid="quick-action-custom">Custom</button>
                            </div>
                        </div>
                    </section>

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

                    <div class="editor-canvas" data-testid="editor-canvas" data-editor-canvas>
                        <div class="editor-surface" data-editor-surface data-editor-placeholder="Schreibe hier deinen Text."></div>
                        <textarea hidden name="text" data-editor-mirror data-testid="editor-mirror"></textarea>

                        <div class="rewrite-bubble" data-rewrite-bubble data-testid="rewrite-bubble" hidden>
                            <p class="rewrite-focus">
                                <strong data-rewrite-focus data-testid="rewrite-focus"></strong>
                            </p>

                            <div class="rewrite-action-row">
                                <button type="button" class="rewrite-primary-action" data-rewrite-primary-action data-testid="rewrite-primary-action">Wort umschreiben</button>
                                <button type="button" class="rewrite-secondary-action" data-rewrite-secondary-action data-testid="rewrite-secondary-action" hidden>Satz umschreiben</button>
                            </div>

                            <div class="rewrite-overlay" data-rewrite-overlay hidden>
                                <p class="rewrite-status" data-rewrite-status data-testid="rewrite-status" aria-live="polite"></p>
                                <div class="rewrite-options" data-rewrite-options></div>
                            </div>
                        </div>
                    </div>

                    <section class="rewrite-diff-panel" data-rewrite-diff-panel data-testid="rewrite-diff-panel" hidden>
                        <div class="rewrite-diff-head">
                            <div>
                                <p class="eyebrow">Rewrite Diff</p>
                                <h3>Alt gegen Neu</h3>
                            </div>

                            <button type="button" class="rewrite-diff-undo" data-rewrite-diff-undo data-testid="rewrite-diff-undo">Rewrite rueckgaengig machen</button>
                        </div>

                        <div class="rewrite-diff-grid">
                            <article class="rewrite-diff-column">
                                <strong>Vorher</strong>
                                <div class="rewrite-diff-content" data-rewrite-diff-before data-testid="rewrite-diff-before"></div>
                            </article>

                            <article class="rewrite-diff-column">
                                <strong>Nachher</strong>
                                <div class="rewrite-diff-content" data-rewrite-diff-after data-testid="rewrite-diff-after"></div>
                            </article>
                        </div>
                    </section>
                </div>
            </div>
        </section>

        <aside class="workspace-sidebar">
            <section class="panel correction-panel" data-testid="correction-panel" data-correction-panel data-correction-state="idle">
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
                            <strong>Produktionsnaeher statt groesser</strong>
                            Fehlerseiten, API-Fehler und Trace-IDs ziehen jetzt dieselbe Betriebslogik durch die vorhandenen Korrektur-, Rewrite- und Importfluesse.
                        </div>
                        <div class="stack-item">
                            <strong>Bewusst weiterhin begrenzt</strong>
                            Keine Uploads in den Advisor-Bestand und keine weiteren Quick Actions ausserhalb der bereits freigeschalteten Aktionen.
                        </div>
                    </div>
                </div>
            </section>

            <section class="panel advisor-panel" data-testid="advisor-panel" data-advisor-panel data-advisor-state="idle">
                <div class="panel-body">
                    <p class="eyebrow">Advisor Catalog</p>
                    <h2>Referenzdokumente</h2>
                    <p class="panel-copy">Die Auswahl bleibt mehrfach moeglich, Treffer laufen waehrend des Streams direkt in die Ergebnisliste und nutzen dasselbe konsistente Fehler- und Auth-Verhalten wie die uebrigen API-Flows.</p>

                    <div class="advisor-toolbar">
                        <p class="advisor-status" data-advisor-status data-testid="advisor-status" aria-live="polite">
                            ${page.advisorDocs().size()} Dokumente verfuegbar. Waehle Referenzen, schreibe Text im Editor und starte dann die Advisor-Pruefung.
                        </p>
                        <button type="button" class="advisor-validate" data-advisor-validate data-testid="advisor-validate">Pruefung starten</button>
                    </div>

                    <div class="advisor-list" data-testid="advisor-doc-list">
                        @for (var advisorDoc : page.advisorDocs())
                            <article class="advisor-item" data-testid="advisor-doc-item">
                                <label class="advisor-select">
                                    <input type="checkbox" name="advisor-docs" value="${advisorDoc.name()}" data-testid="advisor-doc-checkbox">
                                    <span>Auswaehlen</span>
                                </label>

                                <div class="advisor-copy">
                                    <strong data-testid="advisor-doc-title">${advisorDoc.title()}</strong>
                                    <p>${advisorDoc.summary()}</p>
                                    <span class="advisor-source">${advisorDoc.source()}</span>
                                </div>

                                <div class="advisor-links">
                                    <a class="advisor-link" href="${advisorDoc.documentUrl()}" target="_blank" rel="noopener noreferrer" data-testid="advisor-doc-open">PDF anzeigen</a>
                                    <a class="advisor-link advisor-link-secondary" href="${advisorDoc.documentUrl()}" download="${advisorDoc.name()}.pdf" data-testid="advisor-doc-download">PDF laden</a>
                                </div>
                            </article>
                        @endfor
                    </div>

                    <section class="advisor-results-panel" data-testid="advisor-results-panel">
                        <div class="advisor-results-head">
                            <div>
                                <p class="eyebrow">Validation Stream</p>
                                <h3>Trefferliste</h3>
                                <p class="panel-copy">Die Liste aktualisiert sich waehrend des Streams. Der Client dedupliziert Treffer ueber einen stabilen Schluessel.</p>
                            </div>

                            <span class="advisor-result-counter" data-advisor-result-count data-testid="advisor-result-count">0 Treffer</span>
                        </div>

                        <p class="advisor-results-empty" data-advisor-result-empty data-testid="advisor-result-empty">
                            Noch keine Advisor-Treffer. Starte die Pruefung mit markierten Dokumenten.
                        </p>

                        <div class="advisor-results" data-advisor-result-list data-testid="advisor-result-list"></div>

                        <section class="advisor-detail" data-advisor-result-detail data-testid="advisor-result-detail" hidden>
                            <div class="advisor-detail-head">
                                <div>
                                    <p class="eyebrow">Ausgewaehlter Treffer</p>
                                    <h3 data-advisor-result-detail-title data-testid="advisor-result-detail-title"></h3>
                                </div>

                                <a class="advisor-link" href="#" target="_blank" rel="noopener noreferrer" data-advisor-result-detail-link data-testid="advisor-result-detail-link">Dokument oeffnen</a>
                            </div>

                            <p class="advisor-detail-reference" data-advisor-result-detail-reference data-testid="advisor-result-detail-reference"></p>
                            <p class="advisor-detail-match" data-advisor-result-detail-match data-testid="advisor-result-detail-match"></p>
                            <p class="advisor-detail-message" data-advisor-result-detail-message data-testid="advisor-result-detail-message"></p>
                            <blockquote class="advisor-detail-excerpt" data-advisor-result-detail-excerpt data-testid="advisor-result-detail-excerpt"></blockquote>
                            <p class="advisor-detail-suggestion" data-advisor-result-detail-suggestion data-testid="advisor-result-detail-suggestion"></p>
                        </section>
                    </section>

                    <div class="stack-list advisor-footnotes">
                        <div class="stack-item">
                            <strong>Mehrfachauswahl aktiv genutzt</strong>
                            Die Checkboxen steuern jetzt direkt, welche Dokumentregeln in die Advisor-Pruefung einbezogen werden.
                        </div>
                        <div class="stack-item">
                            <strong>Bewusst statisch</strong>
                            Der Katalog liest weiterhin nur projektinterne Demo-Metadaten und PDFs aus <code>src/main/resources/advisor</code>. Uploads erweitern diesen Bestand nicht.
                        </div>
                    </div>
                </div>
            </section>
        </aside>
    </main>
</div>
`)

@import app.textbuddy.web.page.HomePageModel
@import app.textbuddy.document.DocumentOcrLanguage
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
                Phase 03 liefert lokale OCR für gescannte Dokumente, robuste Upload-Grenzen und editorfreundliche HTML-Nachbearbeitung.
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
                <p class="panel-copy">Die Insel bündelt Wort-/Satz-Rewrite, alle neun streamingfähigen Volltext-Rewrites, Dokumentimport, Advisor-Prüfung und konsistente Fehlerführung in einer produktionsnahen Arbeitsfläche.</p>

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
                        <span class="chip">LanguageTool prüft nur geänderte Segmente</span>
                        <span class="chip">Fokussiertes Wort hat Vorrang vor Satzfokus</span>
                        <span class="chip">Dasselbe Bubble-Menü startet Wort-Synonyme oder Satzumformung</span>
                        <span class="chip">Plain Language streamt über SSE direkt in den Editor</span>
                        <span class="chip">Bullet Points nutzt dieselbe Streaming- und Diff-Infrastruktur</span>
                        <span class="chip">Proofread nutzt dieselbe Streaming- und Diff-Infrastruktur</span>
                        <span class="chip">Summarize wählt seine Variante über ein eigenes Dropdown</span>
                        <span class="chip">Formality wählt zwischen formal und informal</span>
                        <span class="chip">Social Media wählt zwischen Bluesky, Instagram und LinkedIn</span>
                        <span class="chip">Medium wählt zwischen E-Mail, offiziellem Schreiben, Präsentation und Bericht</span>
                        <span class="chip">Character Speech wählt zwischen direkter und indirekter Rede</span>
                        <span class="chip">Custom nimmt einen freien Arbeitsauftrag im Rewrite-Bereich entgegen</span>
                        <span class="chip">Rewrite-Diff und kompletter Undo bauen auf derselben Streaming-Basis auf</span>
                        <span class="chip">Dokumentimport gleicht Upload-Formate zwischen UI, Runtime und Backend ab</span>
                        <span class="chip">Lokale OCR unterstützt Deutsch, Englisch, Französisch und Italienisch</span>
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
                                <p class="eyebrow">Dokumentimport</p>
                                <h3>Upload oder Drag-and-Drop</h3>
                                <p class="panel-copy">Unterstützte Dokumente werden standardmässig eingebettet über Kreuzberg mit lokaler OCR verarbeitet. Das zurückgelieferte HTML wird editorfreundlich nachbearbeitet und landet direkt in der bestehenden Tiptap-Insel, ohne Upload-Persistenz ausserhalb des aktuellen Requests. Bei aktiver OIDC-Konfiguration läuft der Endpoint nur für angemeldete Sessions.</p>
                            </div>

                            <p class="document-import-status" data-document-import-status data-testid="document-import-status" aria-live="polite">Bereit für Upload oder Drag-and-Drop.</p>
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
                                <span>Ziehe eine Datei auf diese Fläche oder wähle sie über den Upload-Button aus.</span>
                                <label class="quick-action-select-shell document-import-select-shell" for="document-import-ocr-language">
                                    <span class="quick-action-select-label">OCR-Sprache</span>
                                    <select
                                        id="document-import-ocr-language"
                                        class="quick-action-select document-import-ocr-language"
                                        data-document-import-ocr-language
                                        data-testid="document-import-ocr-language"
                                    >
                                        @for (var ocrLanguage : DocumentOcrLanguage.supportedLanguages())
                                            @if (ocrLanguage == DocumentOcrLanguage.DEFAULT)
                                                <option value="${ocrLanguage.code()}" selected>${ocrLanguage.label()}</option>
                                            @else
                                                <option value="${ocrLanguage.code()}">${ocrLanguage.label()}</option>
                                            @endif
                                        @endfor
                                    </select>
                                </label>
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
                                <p class="panel-copy">Plain Language, Bullet Points, Proofread, Summarize, Formality, Social Media, Medium, Character Speech und Custom bleiben bewusst getrennte Aktionen mit klaren Optionen statt einer generischen Schalter-API.</p>
                            </div>

                            <p class="quick-action-status" data-quick-action-status data-testid="quick-action-status" aria-live="polite">Bereit für Plain Language, Bullet Points, Proofread, Summarize, Formality, Social Media, Medium, Character Speech und Custom.</p>
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
                                        <option value="three_sentence">Drei Sätze</option>
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
                                        <option value="presentation">Präsentation</option>
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

                            <button type="button" class="rewrite-diff-undo" data-rewrite-diff-undo data-testid="rewrite-diff-undo">Rewrite rückgängig machen</button>
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
                    <p class="eyebrow">Korrekturpanel</p>
                    <h2>Textkorrektur</h2>
                    <p class="panel-copy">Die Insel steuert die gemeinsame Textsprache für Korrektur und Quick Actions, das lokale Wörterbuch sowie differenzierte Re-Checks. Der Server bleibt beim bestehenden Korrektur-Endpoint.</p>

                    <div class="correction-controls" data-testid="correction-controls">
                        <label class="control-stack" for="correction-language">
                            <span class="control-label">Textsprache</span>
                            <select id="correction-language" class="control-select" data-correction-language data-testid="correction-language">
                                <option value="auto" selected>Automatisch</option>
                                <option value="de-CH">Deutsch (Schweiz)</option>
                                <option value="fr">Französisch</option>
                                <option value="it">Italienisch</option>
                                <option value="en-US">Englisch (USA)</option>
                                <option value="en-GB">Englisch (UK)</option>
                            </select>
                        </label>

                        <div class="dictionary-shell" data-testid="dictionary-shell">
                            <div class="dictionary-head">
                                <strong>Lokales Wörterbuch</strong>
                                <span>Nur browserlokal, ohne Serverpersistenz</span>
                            </div>

                            <form class="dictionary-form" data-dictionary-form data-testid="dictionary-form">
                                <input type="text" class="dictionary-input" data-dictionary-input data-testid="dictionary-input" placeholder="Wort hinzufügen" autocomplete="off" />
                                <button type="submit" class="dictionary-submit" data-testid="dictionary-submit">Hinzufügen</button>
                            </form>

                            <p class="dictionary-empty" data-dictionary-empty data-testid="dictionary-empty">Noch keine lokalen Wörter gespeichert.</p>
                            <ul class="dictionary-list" data-dictionary-list data-testid="dictionary-list"></ul>
                        </div>
                    </div>

                    <section class="text-stats-panel" data-text-stats-panel data-testid="text-stats-panel">
                        <div class="text-stats-head">
                            <div>
                                <p class="eyebrow">Textstatistik</p>
                                <h3>Lesbarkeit und Umfang</h3>
                            </div>
                        </div>

                        <div class="text-stats-grid">
                            <article class="text-stats-item">
                                <span>Zeichen</span>
                                <strong data-text-stats="characters" data-testid="text-stats-characters">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>Wörter</span>
                                <strong data-text-stats="words" data-testid="text-stats-words">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>Silben</span>
                                <strong data-text-stats="syllables" data-testid="text-stats-syllables">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>Sätze</span>
                                <strong data-text-stats="sentences" data-testid="text-stats-sentences">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>Ø Satzlänge</span>
                                <strong data-text-stats="avg-sentence-length" data-testid="text-stats-avg-sentence-length">0.0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>Ø Silben/Wort</span>
                                <strong data-text-stats="avg-syllables-per-word" data-testid="text-stats-avg-syllables-per-word">0.0</strong>
                            </article>
                        </div>

                        <div class="text-stats-flesch">
                            <div class="text-stats-flesch-head">
                                <span>Flesch-Lesbarkeitsindex</span>
                                <strong data-text-stats="flesch" data-testid="text-stats-flesch">0.0</strong>
                            </div>
                            <div class="text-stats-flesch-bar" role="img" aria-label="Flesch-Lesbarkeit">
                                <span data-text-stats-flesch-fill></span>
                            </div>
                            <p class="text-stats-flesch-label" data-text-stats-flesch-label data-testid="text-stats-flesch-label">Keine Bewertung</p>
                        </div>
                    </section>

                    <p class="problem-status" data-correction-status data-testid="correction-status">Schreibe Text, um Korrekturen zu sehen.</p>
                    <div class="problem-list" data-correction-list data-testid="correction-problem-list"></div>

                    <div class="stack-list problems-footnotes">
                        <div class="stack-item">
                            <strong>Produktionsnäher statt grösser</strong>
                            Fehlerseiten, API-Fehler und Trace-IDs ziehen jetzt dieselbe Betriebslogik durch die vorhandenen Korrektur-, Rewrite- und Importflüsse.
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
                    <p class="eyebrow">Advisor-Katalog</p>
                    <h2>Referenzdokumente</h2>
                    <p class="panel-copy">Dokumente lassen sich direkt im Browser anzeigen, seitenweise prüfen und mit Zoom steuern. Treffer laufen während des Streams in dieselbe konsistente Ergebnisansicht.</p>

                    <div class="advisor-toolbar">
                        <p class="advisor-status" data-advisor-status data-testid="advisor-status" aria-live="polite">
                            ${page.advisorDocs().size()} Dokumente verfügbar. Wähle Referenzen, schreibe Text im Editor und starte dann die Advisor-Prüfung.
                        </p>
                        <button type="button" class="advisor-validate" data-advisor-validate data-testid="advisor-validate">Prüfung starten</button>
                    </div>

                    <div class="advisor-list" data-testid="advisor-doc-list">
                        @for (var advisorDoc : page.advisorDocs())
                            <article class="advisor-item" data-testid="advisor-doc-item">
                                <label class="advisor-select">
                                    <input type="checkbox" name="advisor-docs" value="${advisorDoc.name()}" data-testid="advisor-doc-checkbox">
                                    <span>Auswählen</span>
                                </label>

                                <div class="advisor-copy">
                                    <strong data-testid="advisor-doc-title">${advisorDoc.title()}</strong>
                                    <p>${advisorDoc.summary()}</p>
                                    <span class="advisor-source">${advisorDoc.source()}</span>
                                </div>

                                <div class="advisor-links">
                                    <button
                                        type="button"
                                        class="advisor-link"
                                        data-advisor-open
                                        data-advisor-doc-url="${advisorDoc.documentUrl()}"
                                        data-advisor-doc-title="${advisorDoc.title()}"
                                        data-advisor-doc-name="${advisorDoc.name()}"
                                        data-testid="advisor-doc-open"
                                    >Im Viewer öffnen</button>
                                    <a class="advisor-link" href="${advisorDoc.documentUrl()}" target="_blank" rel="noopener noreferrer" data-testid="advisor-doc-open-tab">Im neuen Tab</a>
                                    <a class="advisor-link advisor-link-secondary" href="${advisorDoc.documentUrl()}" download="${advisorDoc.name()}.pdf" data-testid="advisor-doc-download">PDF herunterladen</a>
                                </div>
                            </article>
                        @endfor
                    </div>

                    <section class="advisor-results-panel" data-testid="advisor-results-panel">
                        <div class="advisor-results-head">
                            <div>
                                <p class="eyebrow">Validierungsstream</p>
                                <h3>Trefferliste</h3>
                                <p class="panel-copy">Die Liste aktualisiert sich während des Streams. Der Client dedupliziert Treffer über einen stabilen Schlüssel.</p>
                            </div>

                            <span class="advisor-result-counter" data-advisor-result-count data-testid="advisor-result-count">0 Treffer</span>
                        </div>

                        <p class="advisor-results-empty" data-advisor-result-empty data-testid="advisor-result-empty">
                            Noch keine Advisor-Treffer. Starte die Prüfung mit markierten Dokumenten.
                        </p>

                        <div class="advisor-results" data-advisor-result-list data-testid="advisor-result-list"></div>

                        <section class="advisor-detail" data-advisor-result-detail data-testid="advisor-result-detail" hidden>
                            <div class="advisor-detail-head">
                                <div>
                                    <p class="eyebrow">Ausgewählter Treffer</p>
                                    <h3 data-advisor-result-detail-title data-testid="advisor-result-detail-title"></h3>
                                </div>

                                <div class="advisor-detail-actions">
                                    <button type="button" class="advisor-link" data-advisor-result-detail-open data-testid="advisor-result-detail-open" hidden>Treffer im Viewer öffnen</button>
                                    <a class="advisor-link advisor-link-secondary" href="#" target="_blank" rel="noopener noreferrer" data-advisor-result-detail-link data-testid="advisor-result-detail-link" hidden>Im neuen Tab öffnen</a>
                                </div>
                            </div>

                            <p class="advisor-detail-reference" data-advisor-result-detail-reference data-testid="advisor-result-detail-reference"></p>
                            <p class="advisor-detail-match" data-advisor-result-detail-match data-testid="advisor-result-detail-match"></p>
                            <p class="advisor-detail-message" data-advisor-result-detail-message data-testid="advisor-result-detail-message"></p>
                            <blockquote class="advisor-detail-excerpt" data-advisor-result-detail-excerpt data-testid="advisor-result-detail-excerpt"></blockquote>
                            <p class="advisor-detail-suggestion" data-advisor-result-detail-suggestion data-testid="advisor-result-detail-suggestion"></p>
                        </section>
                    </section>

                    <section class="advisor-pdf-viewer" data-advisor-pdf-viewer data-testid="advisor-pdf-viewer" hidden>
                        <div class="advisor-pdf-head">
                            <div>
                                <p class="eyebrow">PDF-Viewer</p>
                                <h3 data-advisor-pdf-title data-testid="advisor-pdf-title">Dokumentansicht</h3>
                            </div>

                            <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-close data-testid="advisor-pdf-close">Viewer schließen</button>
                        </div>

                        <div class="advisor-pdf-controls">
                            <div class="advisor-pdf-control-group">
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-prev data-testid="advisor-pdf-prev">Seite zurück</button>
                                <label class="advisor-pdf-page-shell" for="advisor-pdf-page-input">
                                    <span>Seite</span>
                                    <input id="advisor-pdf-page-input" type="number" min="1" value="1" data-advisor-pdf-page-input data-testid="advisor-pdf-page-input">
                                </label>
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-next data-testid="advisor-pdf-next">Seite vor</button>
                            </div>

                            <div class="advisor-pdf-control-group">
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-zoom-out data-testid="advisor-pdf-zoom-out">Zoom −</button>
                                <span class="advisor-pdf-zoom-label" data-advisor-pdf-zoom-label data-testid="advisor-pdf-zoom-label">100%</span>
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-zoom-in data-testid="advisor-pdf-zoom-in">Zoom +</button>
                                <a class="advisor-link" href="#" data-advisor-pdf-download data-testid="advisor-pdf-download">PDF herunterladen</a>
                            </div>
                        </div>

                        <iframe
                            class="advisor-pdf-frame"
                            data-advisor-pdf-frame
                            data-testid="advisor-pdf-frame"
                            title="Advisor PDF Viewer"
                            loading="lazy"
                        ></iframe>
                    </section>

                    <div class="stack-list advisor-footnotes">
                        <div class="stack-item">
                            <strong>Mehrfachauswahl aktiv genutzt</strong>
                            Die Checkboxen steuern jetzt direkt, welche Dokumentregeln in die Advisor-Prüfung einbezogen werden.
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

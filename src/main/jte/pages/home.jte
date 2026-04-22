@import app.textbuddy.web.page.HomePageModel
@import app.textbuddy.document.DocumentOcrLanguage
@param HomePageModel page

@template.layout.main(title = page.title(), lang = page.uiLocaleTag(), skipLinkLabel = page.t("layout.skipLink"), content = @`
<div class="page-shell">
    <header class="app-header panel" data-testid="shell-header">
        <div>
            <p class="eyebrow">${page.t("home.header.eyebrow")}</p>
            <h1>${page.t("home.header.brand")}</h1>
            <p>${page.subtitle()}</p>
        </div>
        <div class="status-strip">
            <div class="status-pill" data-testid="release-status-pill">
                ${page.t("home.releaseStatus")}
            </div>
            <div class="status-pill" data-testid="auth-status-pill">
                <strong>${page.auth().statusTitle()}</strong>
                <span>${page.auth().statusMessage()}</span>
            </div>
            @if (page.auth().enabled() && page.auth().authenticated())
                <div class="status-pill status-pill-account" data-testid="auth-account-pill">
                    <strong data-testid="auth-user">${page.auth().userDisplayName()}</strong>
                    <form method="post" action="/logout" class="auth-form" data-testid="auth-logout-form">
                        <button type="submit" class="inline-action" data-testid="auth-logout">${page.t("auth.logout")}</button>
                    </form>
                </div>
            @elseif (page.auth().enabled())
                <div class="status-pill status-pill-account" data-testid="auth-login-pill">
                    <strong>${page.t("auth.login.title")}</strong>
                    <a class="inline-action" href="${page.auth().loginUrl()}" data-testid="auth-login-link">${page.t("auth.login.link")}</a>
                </div>
            @endif
            <nav class="status-pill status-pill-account locale-switcher" aria-label="${page.t("locale.switcher.ariaLabel")}" data-testid="locale-switcher">
                @for (var language : page.supportedUiLanguages())
                    @if (language.equals(page.uiLocaleLanguage()))
                        <strong>${language.toUpperCase()}</strong>
                    @else
                        <a class="inline-action" href="/?lang=${language}" hreflang="${language}">${language.toUpperCase()}</a>
                    @endif
                @endfor
            </nav>
        </div>
    </header>

    <main id="main-content" class="workspace-grid" tabindex="-1">
        <section class="panel" data-testid="editor-shell" aria-labelledby="editor-shell-title">
            <div class="panel-body">
                <p class="eyebrow">${page.t("editor.shell.eyebrow")}</p>
                <h2 id="editor-shell-title">${page.t("editor.shell.title")}</h2>
                <p class="panel-copy">${page.t("editor.shell.copy")}</p>

                <div
                    id="editor-island-root"
                    class="editor-root"
                    data-testid="editor-island-root"
                    data-auth-enabled="${Boolean.toString(page.auth().enabled())}"
                    data-authenticated="${Boolean.toString(page.auth().authenticated())}"
                    data-auth-login-url="${page.auth().loginUrl()}"
                    data-ui-locale="${page.uiLocaleLanguage()}"
                    data-ui-messages="${page.uiMessagesJson()}"
                >
                    <details class="workspace-notes" data-testid="workspace-notes">
                        <summary>${page.t("editor.notes.summary")}</summary>
                        <div class="chip-row chip-row-compact">
                            <span class="chip">${page.t("editor.notes.chip.sse")}</span>
                            <span class="chip">${page.t("editor.notes.chip.segments")}</span>
                            <span class="chip">${page.t("editor.notes.chip.dictionary")}</span>
                            <span class="chip">${page.t("editor.notes.chip.advisor")}</span>
                            <span class="chip">${page.t("editor.notes.chip.import")}</span>
                            <span class="chip">${page.t("editor.notes.chip.oidc")}</span>
                        </div>
                    </details>

                    <div class="editor-toolbar" data-testid="editor-toolbar">
                        <div class="editor-toolbar-group">
                            <button type="button" class="editor-button" data-editor-action="undo" data-testid="editor-undo" disabled>${page.t("editor.undo")}</button>
                            <button type="button" class="editor-button" data-editor-action="redo" data-testid="editor-redo" disabled>${page.t("editor.redo")}</button>
                        </div>

                        <div class="editor-counter-bar" aria-live="polite">
                            <span class="counter-pill"><strong data-editor-count="characters" data-testid="editor-character-count">0</strong> ${page.t("editor.count.characters")}</span>
                            <span class="counter-pill"><strong data-editor-count="words" data-testid="editor-word-count">0</strong> ${page.t("editor.count.words")}</span>
                        </div>
                    </div>

                    <div class="editor-canvas" data-testid="editor-canvas" data-editor-canvas>
                        <div class="editor-surface" data-editor-surface data-editor-placeholder="${page.t("editor.placeholder")}"></div>
                        <textarea hidden name="text" data-editor-mirror data-testid="editor-mirror"></textarea>

                        <div class="rewrite-bubble" data-rewrite-bubble data-testid="rewrite-bubble" hidden>
                            <p class="rewrite-focus">
                                <strong data-rewrite-focus data-testid="rewrite-focus"></strong>
                            </p>

                            <div class="rewrite-action-row">
                                <button type="button" class="rewrite-primary-action" data-rewrite-primary-action data-testid="rewrite-primary-action">${page.t("rewrite.wordAction")}</button>
                                <button type="button" class="rewrite-secondary-action" data-rewrite-secondary-action data-testid="rewrite-secondary-action" hidden>${page.t("rewrite.sentenceAction")}</button>
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
                                <p class="eyebrow">${page.t("rewrite.diff.eyebrow")}</p>
                                <h3>${page.t("rewrite.diff.title")}</h3>
                            </div>

                            <button type="button" class="rewrite-diff-undo" data-rewrite-diff-undo data-testid="rewrite-diff-undo">${page.t("rewrite.diff.undo")}</button>
                        </div>

                        <div class="rewrite-diff-grid">
                            <article class="rewrite-diff-column">
                                <strong>${page.t("rewrite.diff.before")}</strong>
                                <div class="rewrite-diff-content" data-rewrite-diff-before data-testid="rewrite-diff-before"></div>
                            </article>

                            <article class="rewrite-diff-column">
                                <strong>${page.t("rewrite.diff.after")}</strong>
                                <div class="rewrite-diff-content" data-rewrite-diff-after data-testid="rewrite-diff-after"></div>
                            </article>
                        </div>
                    </section>

                    <section
                        class="quick-action-panel"
                        data-quick-action-panel
                        data-testid="quick-action-panel"
                        data-quick-action-state="idle"
                        data-quick-action-selected-action="plain-language"
                        aria-labelledby="quick-action-title"
                    >
                        <div class="quick-action-panel-head">
                            <div>
                                <p class="eyebrow">${page.t("quickAction.eyebrow")}</p>
                                <h3 id="quick-action-title">${page.t("quickAction.title")}</h3>
                                <p class="panel-copy">${page.t("quickAction.copy")}</p>
                            </div>

                            <p class="quick-action-status" data-quick-action-status data-testid="quick-action-status" role="status" aria-live="polite" aria-atomic="true">${page.t("quickAction.status.idle")}</p>
                        </div>

                        <div class="quick-action-layout">
                            <div class="quick-action-groups">
                                <article class="quick-action-group" data-testid="quick-action-group-editing">
                                    <h4>${page.t("quickAction.group.editing")}</h4>
                                    <div class="quick-action-group-buttons">
                                        <button type="button" class="quick-action-button" data-quick-action="plain-language" data-testid="quick-action-plain-language">${page.t("quickAction.action.plainLanguage")}</button>
                                        <button type="button" class="quick-action-button" data-quick-action="proofread" data-testid="quick-action-proofread">${page.t("quickAction.action.proofread")}</button>
                                        <button type="button" class="quick-action-button" data-quick-action="bullet-points" data-testid="quick-action-bullet-points">${page.t("quickAction.action.bulletPoints")}</button>
                                        <button type="button" class="quick-action-button" data-quick-action="summarize" data-testid="quick-action-summarize">${page.t("quickAction.action.summarize")}</button>
                                    </div>
                                </article>

                                <article class="quick-action-group" data-testid="quick-action-group-style-channel">
                                    <h4>${page.t("quickAction.group.style")}</h4>
                                    <div class="quick-action-group-buttons">
                                        <button type="button" class="quick-action-button" data-quick-action="formality" data-testid="quick-action-formality">${page.t("quickAction.action.formality")}</button>
                                        <button type="button" class="quick-action-button" data-quick-action="social-media" data-testid="quick-action-social-media">${page.t("quickAction.action.socialMedia")}</button>
                                        <button type="button" class="quick-action-button" data-quick-action="medium" data-testid="quick-action-medium">${page.t("quickAction.action.medium")}</button>
                                    </div>
                                </article>

                                <article class="quick-action-group" data-testid="quick-action-group-narrative">
                                    <h4>${page.t("quickAction.group.narrative")}</h4>
                                    <div class="quick-action-group-buttons">
                                        <button type="button" class="quick-action-button" data-quick-action="character-speech" data-testid="quick-action-character-speech">${page.t("quickAction.action.characterSpeech")}</button>
                                    </div>
                                </article>

                                <article class="quick-action-group" data-testid="quick-action-group-custom">
                                    <h4>${page.t("quickAction.group.custom")}</h4>
                                    <div class="quick-action-group-buttons">
                                        <button type="button" class="quick-action-button" data-quick-action="custom" data-testid="quick-action-custom">${page.t("quickAction.action.custom")}</button>
                                    </div>
                                </article>
                            </div>

                            <section class="quick-action-config-shell" data-testid="quick-action-config-shell">
                                <div class="quick-action-config-head">
                                    <p class="eyebrow">${page.t("quickAction.config.eyebrow")}</p>
                                    <h4 data-quick-action-active-label data-testid="quick-action-active-label">${page.t("quickAction.action.plainLanguage")}</h4>
                                </div>
                                <p class="quick-action-config-copy">${page.t("quickAction.config.copy")}</p>

                                <div class="quick-action-configs">
                                    <div class="quick-action-config" data-quick-action-config="plain-language" data-testid="quick-action-config-plain-language">
                                        <p>${page.t("quickAction.config.plainLanguage")}</p>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="proofread" data-testid="quick-action-config-proofread" hidden>
                                        <p>${page.t("quickAction.config.proofread")}</p>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="bullet-points" data-testid="quick-action-config-bullet-points" hidden>
                                        <p>${page.t("quickAction.config.bulletPoints")}</p>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="summarize" data-testid="quick-action-config-summarize" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-summarize-option">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.summarize.label")}</span>
                                            <select id="quick-action-summarize-option" class="quick-action-select" data-quick-action-option="summarize" data-testid="quick-action-summarize-option">
                                                <option value="sentence" selected>${page.t("quickAction.option.summarize.sentence")}</option>
                                                <option value="three_sentence">${page.t("quickAction.option.summarize.threeSentence")}</option>
                                                <option value="paragraph">${page.t("quickAction.option.summarize.paragraph")}</option>
                                                <option value="page">${page.t("quickAction.option.summarize.page")}</option>
                                                <option value="management_summary">Management Summary</option>
                                            </select>
                                        </label>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="formality" data-testid="quick-action-config-formality" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-formality-option">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.formality.label")}</span>
                                            <select id="quick-action-formality-option" class="quick-action-select" data-quick-action-option="formality" data-testid="quick-action-formality-option">
                                                <option value="formal" selected>${page.t("quickAction.option.formality.formal")}</option>
                                                <option value="informal">${page.t("quickAction.option.formality.informal")}</option>
                                            </select>
                                        </label>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="social-media" data-testid="quick-action-config-social-media" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-social-media-option">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.socialMedia.label")}</span>
                                            <select id="quick-action-social-media-option" class="quick-action-select" data-quick-action-option="social-media" data-testid="quick-action-social-media-option">
                                                <option value="bluesky" selected>Bluesky</option>
                                                <option value="instagram">Instagram</option>
                                                <option value="linkedin">LinkedIn</option>
                                            </select>
                                        </label>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="medium" data-testid="quick-action-config-medium" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-medium-option">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.medium.label")}</span>
                                            <select id="quick-action-medium-option" class="quick-action-select" data-quick-action-option="medium" data-testid="quick-action-medium-option">
                                                <option value="email" selected>${page.t("quickAction.option.medium.email")}</option>
                                                <option value="official_letter">${page.t("quickAction.option.medium.officialLetter")}</option>
                                                <option value="presentation">${page.t("quickAction.option.medium.presentation")}</option>
                                                <option value="report">${page.t("quickAction.option.medium.report")}</option>
                                            </select>
                                        </label>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="character-speech" data-testid="quick-action-config-character-speech" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-character-speech-option">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.characterSpeech.label")}</span>
                                            <select id="quick-action-character-speech-option" class="quick-action-select" data-quick-action-option="character-speech" data-testid="quick-action-character-speech-option">
                                                <option value="direct_speech" selected>${page.t("quickAction.option.characterSpeech.direct")}</option>
                                                <option value="indirect_speech">${page.t("quickAction.option.characterSpeech.indirect")}</option>
                                            </select>
                                        </label>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="custom" data-testid="quick-action-config-custom" hidden>
                                        <label class="quick-action-select-shell" for="quick-action-custom-prompt">
                                            <span class="quick-action-select-label">${page.t("quickAction.option.custom.label")}</span>
                                            <textarea id="quick-action-custom-prompt" class="quick-action-textarea" data-quick-action-prompt="custom" data-testid="quick-action-custom-prompt" rows="4" maxlength="400" placeholder="${page.t("quickAction.option.custom.placeholder")}"></textarea>
                                        </label>
                                    </div>
                                </div>

                                <button type="button" class="quick-action-apply" data-quick-action-run data-testid="quick-action-run">${page.t("quickAction.run")}</button>
                            </section>
                        </div>
                    </section>

                    <section
                        class="document-import-panel"
                        data-document-import-panel
                        data-document-import-state="idle"
                        data-document-import-labels="${String.join(", ", page.documentImportFormats())}"
                        data-testid="document-import-panel"
                        aria-labelledby="document-import-title"
                    >
                        <div class="document-import-head">
                            <div>
                                <p class="eyebrow">${page.t("import.eyebrow")}</p>
                                <h3 id="document-import-title">${page.t("import.title")}</h3>
                                <p class="panel-copy">${page.t("import.copy")}</p>
                            </div>

                            <p class="document-import-status" data-document-import-status data-testid="document-import-status" role="status" aria-live="polite" aria-atomic="true">${page.t("import.status.idle")}</p>
                        </div>

                        <div class="document-import-body">
                            <div
                                class="document-import-dropzone"
                                data-document-import-dropzone
                                data-dragging="false"
                                data-busy="false"
                                data-testid="document-import-dropzone"
                                aria-label="${page.t("import.dropzone.ariaLabel")}"
                                aria-describedby="document-import-dropzone-hint"
                                aria-disabled="false"
                                aria-busy="false"
                            >
                                <input
                                    id="document-import-input"
                                    class="document-import-input"
                                    data-document-import-input
                                    data-testid="document-import-input"
                                    type="file"
                                    accept="${page.documentImportAccept()}"
                                    aria-hidden="true"
                                    tabindex="-1"
                                >
                                <strong>${page.t("import.dropzone.title")}</strong>
                                <span id="document-import-dropzone-hint">${page.t("import.dropzone.hint")}</span>
                                <label class="quick-action-select-shell document-import-select-shell" for="document-import-ocr-language">
                                    <span class="quick-action-select-label">${page.t("import.ocr.label")}</span>
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

                            <button type="button" class="editor-button document-import-button" data-document-import-button data-testid="document-import-button">${page.t("import.button.start")}</button>
                        </div>
                    </section>
                </div>
            </div>
        </section>

        <aside class="workspace-sidebar">
            <section class="panel correction-panel" data-testid="correction-panel" data-correction-panel data-correction-state="idle" aria-labelledby="correction-panel-title">
                <div class="panel-body">
                    <p class="eyebrow">${page.t("correction.eyebrow")}</p>
                    <h2 id="correction-panel-title">${page.t("correction.title")}</h2>
                    <p class="panel-copy">${page.t("correction.copy")}</p>

                    <div class="correction-controls" data-testid="correction-controls">
                        <label class="control-stack" for="correction-language">
                            <span class="control-label">${page.t("correction.language.label")}</span>
                            <select id="correction-language" class="control-select" data-correction-language data-testid="correction-language">
                                <option value="auto" selected>${page.t("correction.language.auto")}</option>
                                <option value="de-CH">${page.t("correction.language.deCH")}</option>
                                <option value="fr">${page.t("correction.language.fr")}</option>
                                <option value="it">${page.t("correction.language.it")}</option>
                                <option value="en-US">${page.t("correction.language.enUS")}</option>
                                <option value="en-GB">${page.t("correction.language.enGB")}</option>
                            </select>
                        </label>

                        <div class="dictionary-shell" data-testid="dictionary-shell">
                            <div class="dictionary-head">
                                <strong>${page.t("dictionary.title")}</strong>
                                <span>${page.t("dictionary.subtitle")}</span>
                            </div>

                            <form class="dictionary-form" data-dictionary-form data-testid="dictionary-form">
                                <label class="sr-only" for="dictionary-input">${page.t("dictionary.input.label")}</label>
                                <input id="dictionary-input" type="text" class="dictionary-input" data-dictionary-input data-testid="dictionary-input" placeholder="${page.t("dictionary.input.placeholder")}" autocomplete="off" />
                                <button type="submit" class="dictionary-submit" data-testid="dictionary-submit">${page.t("dictionary.add")}</button>
                            </form>

                            <p class="dictionary-empty" data-dictionary-empty data-testid="dictionary-empty">${page.t("dictionary.empty")}</p>
                            <ul class="dictionary-list" data-dictionary-list data-testid="dictionary-list"></ul>
                        </div>
                    </div>

                    <section class="text-stats-panel" data-text-stats-panel data-testid="text-stats-panel">
                        <div class="text-stats-head">
                            <div>
                                <p class="eyebrow">${page.t("stats.eyebrow")}</p>
                                <h3>${page.t("stats.title")}</h3>
                            </div>
                        </div>

                        <div class="text-stats-grid">
                            <article class="text-stats-item">
                                <span>${page.t("stats.characters")}</span>
                                <strong data-text-stats="characters" data-testid="text-stats-characters">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>${page.t("stats.words")}</span>
                                <strong data-text-stats="words" data-testid="text-stats-words">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>${page.t("stats.syllables")}</span>
                                <strong data-text-stats="syllables" data-testid="text-stats-syllables">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>${page.t("stats.sentences")}</span>
                                <strong data-text-stats="sentences" data-testid="text-stats-sentences">0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>${page.t("stats.avgSentenceLength")}</span>
                                <strong data-text-stats="avg-sentence-length" data-testid="text-stats-avg-sentence-length">0.0</strong>
                            </article>
                            <article class="text-stats-item">
                                <span>${page.t("stats.avgSyllablesPerWord")}</span>
                                <strong data-text-stats="avg-syllables-per-word" data-testid="text-stats-avg-syllables-per-word">0.0</strong>
                            </article>
                        </div>

                        <div class="text-stats-flesch">
                            <div class="text-stats-flesch-head">
                                <span>${page.t("stats.flesch.label")}</span>
                                <strong data-text-stats="flesch" data-testid="text-stats-flesch">0.0</strong>
                            </div>
                            <div class="text-stats-flesch-bar" role="img" aria-label="${page.t("stats.flesch.ariaLabel")}">
                                <span data-text-stats-flesch-fill></span>
                            </div>
                            <p class="text-stats-flesch-label" data-text-stats-flesch-label data-testid="text-stats-flesch-label">${page.t("stats.flesch.none")}</p>
                        </div>
                    </section>

                    <p class="problem-status" data-correction-status data-testid="correction-status" role="status" aria-live="polite" aria-atomic="true">${page.t("correction.status.idle")}</p>
                    <div class="problem-list" data-correction-list data-testid="correction-problem-list" role="list" aria-label="${page.t("correction.problemList.ariaLabel")}"></div>

                    <div class="stack-list problems-footnotes">
                        <div class="stack-item">
                            <strong>${page.t("correction.note.production.title")}</strong>
                            ${page.t("correction.note.production.body")}
                        </div>
                        <div class="stack-item">
                            <strong>${page.t("correction.note.scope.title")}</strong>
                            ${page.t("correction.note.scope.body")}
                        </div>
                    </div>
                </div>
            </section>

            <section class="panel advisor-panel" data-testid="advisor-panel" data-advisor-panel data-advisor-state="idle" aria-labelledby="advisor-panel-title">
                <div class="panel-body">
                    <p class="eyebrow">${page.t("advisor.eyebrow")}</p>
                    <h2 id="advisor-panel-title">${page.t("advisor.title")}</h2>
                    <p class="panel-copy">${page.t("advisor.copy")}</p>

                    <div class="advisor-toolbar">
                        <p class="advisor-status" data-advisor-status data-testid="advisor-status" role="status" aria-live="polite" aria-atomic="true">
                            ${page.advisorDocs().size()} ${page.t("advisor.docsAvailable")} ${page.t("advisor.idleHint")}
                        </p>
                        <button type="button" class="advisor-validate" data-advisor-validate data-testid="advisor-validate">${page.t("advisor.validate")}</button>
                    </div>

                    <div class="advisor-list" data-testid="advisor-doc-list" role="list" aria-label="${page.t("advisor.docList.ariaLabel")}">
                        @for (var advisorDoc : page.advisorDocs())
                            <article class="advisor-item" data-testid="advisor-doc-item" role="listitem">
                                <label class="advisor-select">
                                    <input type="checkbox" name="advisor-docs" value="${advisorDoc.name()}" data-testid="advisor-doc-checkbox">
                                    <span>${page.t("advisor.select")}</span>
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
                                    >${page.t("advisor.openViewer")}</button>
                                    <a class="advisor-link" href="${advisorDoc.documentUrl()}" target="_blank" rel="noopener noreferrer" data-testid="advisor-doc-open-tab">${page.t("advisor.openTab")}</a>
                                    <a class="advisor-link advisor-link-secondary" href="${advisorDoc.documentUrl()}" download="${advisorDoc.name()}.pdf" data-testid="advisor-doc-download">${page.t("advisor.downloadPdf")}</a>
                                </div>
                            </article>
                        @endfor
                    </div>

                    <section class="advisor-results-panel" data-testid="advisor-results-panel" aria-labelledby="advisor-results-title">
                        <div class="advisor-results-head">
                            <div>
                                <p class="eyebrow">${page.t("advisor.results.eyebrow")}</p>
                                <h3 id="advisor-results-title">${page.t("advisor.results.title")}</h3>
                                <p class="panel-copy">${page.t("advisor.results.copy")}</p>
                            </div>

                            <span class="advisor-result-counter" data-advisor-result-count data-testid="advisor-result-count">0 ${page.t("advisor.results.hits")}</span>
                        </div>

                        <p class="advisor-results-empty" data-advisor-result-empty data-testid="advisor-result-empty">
                            ${page.t("advisor.results.empty")}
                        </p>

                        <div class="advisor-results" data-advisor-result-list data-testid="advisor-result-list" role="list" aria-label="${page.t("advisor.results.ariaLabel")}"></div>

                        <section class="advisor-detail" data-advisor-result-detail data-testid="advisor-result-detail" role="region" aria-live="polite" aria-atomic="true" aria-labelledby="advisor-result-detail-title" hidden>
                            <div class="advisor-detail-head">
                                <div>
                                    <p class="eyebrow">${page.t("advisor.detail.eyebrow")}</p>
                                    <h3 id="advisor-result-detail-title" data-advisor-result-detail-title data-testid="advisor-result-detail-title"></h3>
                                </div>

                                <div class="advisor-detail-actions">
                                    <button type="button" class="advisor-link" data-advisor-result-detail-open data-testid="advisor-result-detail-open" hidden>${page.t("advisor.detail.openViewer")}</button>
                                    <a class="advisor-link advisor-link-secondary" href="#" target="_blank" rel="noopener noreferrer" data-advisor-result-detail-link data-testid="advisor-result-detail-link" hidden>${page.t("advisor.detail.openTab")}</a>
                                </div>
                            </div>

                            <p class="advisor-detail-reference" data-advisor-result-detail-reference data-testid="advisor-result-detail-reference"></p>
                            <p class="advisor-detail-match" data-advisor-result-detail-match data-testid="advisor-result-detail-match"></p>
                            <p class="advisor-detail-message" data-advisor-result-detail-message data-testid="advisor-result-detail-message"></p>
                            <blockquote class="advisor-detail-excerpt" data-advisor-result-detail-excerpt data-testid="advisor-result-detail-excerpt"></blockquote>
                            <p class="advisor-detail-suggestion" data-advisor-result-detail-suggestion data-testid="advisor-result-detail-suggestion"></p>
                        </section>
                    </section>

                    <section class="advisor-pdf-viewer" data-advisor-pdf-viewer data-testid="advisor-pdf-viewer" role="region" aria-labelledby="advisor-pdf-title" hidden>
                        <div class="advisor-pdf-head">
                            <div>
                                <p class="eyebrow">${page.t("viewer.eyebrow")}</p>
                                <h3 id="advisor-pdf-title" data-advisor-pdf-title data-testid="advisor-pdf-title">${page.t("viewer.title")}</h3>
                            </div>

                            <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-close data-testid="advisor-pdf-close">${page.t("viewer.close")}</button>
                        </div>

                        <div class="advisor-pdf-controls">
                            <div class="advisor-pdf-control-group">
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-prev data-testid="advisor-pdf-prev">${page.t("viewer.prev")}</button>
                                <label class="advisor-pdf-page-shell" for="advisor-pdf-page-input">
                                    <span>${page.t("viewer.page")}</span>
                                    <input id="advisor-pdf-page-input" type="number" min="1" value="1" data-advisor-pdf-page-input data-testid="advisor-pdf-page-input">
                                </label>
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-next data-testid="advisor-pdf-next">${page.t("viewer.next")}</button>
                            </div>

                            <div class="advisor-pdf-control-group">
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-zoom-out data-testid="advisor-pdf-zoom-out">${page.t("viewer.zoomOut")}</button>
                                <span class="advisor-pdf-zoom-label" data-advisor-pdf-zoom-label data-testid="advisor-pdf-zoom-label">100%</span>
                                <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-zoom-in data-testid="advisor-pdf-zoom-in">${page.t("viewer.zoomIn")}</button>
                                <a class="advisor-link" href="#" data-advisor-pdf-download data-testid="advisor-pdf-download">${page.t("viewer.download")}</a>
                            </div>
                        </div>

                        <iframe
                            class="advisor-pdf-frame"
                            data-advisor-pdf-frame
                            data-testid="advisor-pdf-frame"
                            title="${page.t("viewer.frameTitle")}"
                            loading="lazy"
                        ></iframe>
                    </section>

                    <div class="stack-list advisor-footnotes">
                        <div class="stack-item">
                            <strong>${page.t("advisor.note.multiSelect.title")}</strong>
                            ${page.t("advisor.note.multiSelect.body")}
                        </div>
                        <div class="stack-item">
                            <strong>${page.t("advisor.note.static.title")}</strong>
                            ${page.t("advisor.note.static.body")}
                        </div>
                    </div>
                </div>
            </section>
        </aside>
    </main>
</div>
`)

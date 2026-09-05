@import app.textbuddy.web.page.HomePageModel
@param HomePageModel page

@template.layout.main(title = page.title(), lang = page.uiLocaleTag(), skipLinkLabel = page.t("layout.skipLink"), content = @`
<div class="app-shell">
    <header class="app-header" data-testid="shell-header">
        <div class="app-header-inner">
            <div class="brand-cluster">
                <span class="app-brand">${page.t("home.header.brand")}</span>
                @if (!page.auth().enabled())
                    <div class="local-mode-status" data-local-mode-status>
                        <button
                                type="button"
                                class="local-mode-trigger"
                                data-local-mode-trigger
                                data-testid="local-mode-trigger"
                                aria-controls="local-mode-popover"
                                aria-expanded="false"
                                aria-label="${page.t("auth.localMode.open")}">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                <circle cx="8" cy="8" r="6.25" stroke="currentColor" stroke-width="1.5"/>
                                <path d="M8 7.25v4" stroke="currentColor" stroke-linecap="round" stroke-width="1.5"/>
                                <circle cx="8" cy="4.75" r=".75" fill="currentColor"/>
                            </svg>
                        </button>
                        <div
                                id="local-mode-popover"
                                class="local-mode-popover"
                                data-local-mode-popover
                                data-testid="local-mode-popover"
                                role="status"
                                aria-labelledby="local-mode-popover-title"
                                aria-describedby="local-mode-popover-message"
                                hidden>
                            <strong id="local-mode-popover-title">${page.auth().statusTitle()}</strong>
                            <p id="local-mode-popover-message">${page.auth().statusMessage()}</p>
                        </div>
                    </div>
                @endif
            </div>
            <nav class="workspace-mode-tabs" role="tablist" aria-label="${page.t("workspace.mode.ariaLabel")}" data-testid="workspace-mode-tabs">
                <button type="button" class="workspace-mode-tab" role="tab" aria-selected="true" data-workspace-mode="transform" data-testid="workspace-mode-transform">
                    <svg class="workspace-mode-tab-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                        <path d="M9.5 2.672a.5.5 0 1 0 1 0V.843a.5.5 0 0 0-1 0zm4.5.035A.5.5 0 0 0 13.293 2L12 3.293a.5.5 0 1 0 .707.707zM7.293 4A.5.5 0 1 0 8 3.293L6.707 2A.5.5 0 0 0 6 2.707zm-.621 2.5a.5.5 0 1 0 0-1H4.843a.5.5 0 1 0 0 1zm8.485 0a.5.5 0 1 0 0-1h-1.829a.5.5 0 0 0 0 1zM13.293 10A.5.5 0 1 0 14 9.293L12.707 8a.5.5 0 0 0-.707.707zM9.5 11.157a.5.5 0 1 0 1 0V9.328a.5.5 0 0 0-1 0zm1.854-5.097a.5.5 0 0 0 0-.706l-.708-.708a.5.5 0 0 0-.707 0L8.646 5.94a.5.5 0 0 0 0 .707l.708.708a.5.5 0 0 0 .707 0l1.293-1.293Zm-3 3a.5.5 0 0 0 0-.706l-.708-.708a.5.5 0 0 0-.707 0L.646 13.94a.5.5 0 0 0 0 .707l.708.708a.5.5 0 0 0 .707 0z"/>
                    </svg>
                    ${page.t("workspace.mode.transform")}
                </button>
                <button type="button" class="workspace-mode-tab" role="tab" aria-selected="false" tabindex="-1" data-workspace-mode="validate" data-testid="workspace-mode-validate">
                    <svg class="workspace-mode-tab-icon" data-icon="file-earmark-check" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                        <path d="M10.854 7.854a.5.5 0 0 0-.708-.708L7.5 9.793 6.354 8.646a.5.5 0 1 0-.708.708l1.5 1.5a.5.5 0 0 0 .708 0z"/>
                        <path d="M14 14V4.5L9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2M9.5 3A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5z"/>
                    </svg>
                    ${page.t("workspace.mode.validate")}
                    <span class="workspace-mode-badge" data-correction-mode-badge data-testid="correction-mode-badge" hidden>0</span>
                </button>
            </nav>
            <div class="header-actions">
            @if (page.auth().enabled() && page.auth().authenticated())
                <div class="account-actions" data-testid="auth-account-pill">
                    <strong data-testid="auth-user">${page.auth().userDisplayName()}</strong>
                    <form method="post" action="/logout" class="auth-form" data-testid="auth-logout-form">
                        <input type="hidden" name="${page.csrfParameterName()}" value="${page.csrfToken()}">
                        <button type="submit" class="inline-action" data-testid="auth-logout">${page.t("auth.logout")}</button>
                    </form>
                </div>
            @elseif (page.auth().enabled())
                <div class="account-actions" data-testid="auth-login-pill">
                    <a class="inline-action" href="${page.auth().loginUrl()}" data-testid="auth-login-link">${page.t("auth.login.link")}</a>
                </div>
            @endif
            </div>
        </div>
    </header>

    <section class="workspace-ribbon" aria-label="${page.t("workspace.ribbon.ariaLabel")}" data-testid="workspace-ribbon">
        <div class="workspace-ribbon-inner" data-workspace-ribbon="transform">
            <div class="ribbon-group ribbon-group-with-title">
                <div class="ribbon-action-row">
                    <label class="sr-only" for="mvp-summary-option">${page.t("quickAction.option.summarize.label")}</label>
                    <select id="mvp-summary-option" class="ribbon-select" data-mvp-summary-option data-testid="mvp-summary-option">
                        <option value="" selected disabled>${page.t("quickAction.action.summarize")}</option>
                        <option value="sentence">${page.t("quickAction.option.summarize.sentence")}</option>
                        <option value="three_sentence">${page.t("quickAction.option.summarize.threeSentence")}</option>
                        <option value="paragraph">${page.t("quickAction.option.summarize.paragraph")}</option>
                        <option value="page">${page.t("quickAction.option.summarize.page")}</option>
                        <option value="management_summary">Management Summary</option>
                    </select>
                </div>
                <span class="ribbon-group-title">${page.t("workspace.ribbon.restructure")}</span>
            </div>
            <span class="ribbon-divider" aria-hidden="true"></span>
            <div class="ribbon-group ribbon-group-with-title">
                <button type="button" class="ribbon-action" data-mvp-quick-action="plain-language" data-testid="mvp-action-plain-language">${page.t("workspace.action.plainLanguage")}</button>
                <span class="ribbon-group-title">${page.t("workspace.ribbon.improve")}</span>
            </div>
        </div>

        <div class="workspace-ribbon-inner" data-workspace-ribbon="validate" hidden>
            <div class="ribbon-group ribbon-group-status">
                <span class="ribbon-group-label">${page.t("correction.title")}</span>
                <p class="ribbon-status" data-correction-ribbon-status data-testid="correction-ribbon-status" role="status" aria-live="polite"></p>
            </div>
            <span class="ribbon-divider" aria-hidden="true"></span>
            <div class="ribbon-group">
                <label class="ribbon-group-label" for="workspace-language">${page.t("correction.language.label")}</label>
                <select id="workspace-language" class="ribbon-select" data-workspace-language data-testid="workspace-language">
                    <option value="auto" selected>${page.t("correction.language.auto")}</option>
                    <option value="de-CH">${page.t("correction.language.deCH")}</option>
                    <option value="fr">${page.t("correction.language.fr")}</option>
                    <option value="it">${page.t("correction.language.it")}</option>
                    <option value="en-US">${page.t("correction.language.enUS")}</option>
                    <option value="en-GB">${page.t("correction.language.enGB")}</option>
                </select>
            </div>
            <div class="ribbon-group ribbon-group-actions">
                <button type="button" class="ribbon-action" data-correction-results-toggle data-testid="correction-results-toggle" aria-expanded="false" disabled>
                    ${page.t("workspace.correction.results")} (<span data-correction-result-count>0</span>)
                </button>
                <button type="button" class="ribbon-action ribbon-action-secondary" data-correction-retry data-testid="correction-retry" hidden>${page.t("workspace.correction.retry")}</button>
            </div>
        </div>
    </section>

    <main id="main-content" class="page-shell" tabindex="-1">
        <div
            id="editor-island-root"
            class="editor-workspace"
            data-testid="editor-island-root"
            data-auth-enabled="${Boolean.toString(page.auth().enabled())}"
            data-authenticated="${Boolean.toString(page.auth().authenticated())}"
            data-auth-login-url="${page.auth().loginUrl()}"
            data-csrf-header="${page.csrfHeaderName()}"
            data-csrf-token="${page.csrfToken()}"
            data-ui-locale="${page.uiLocaleLanguage()}"
            data-ui-messages="${page.uiMessagesJson()}"
            data-workspace-mode="transform"
            data-workspace-view="editor"
        >
            <section class="panel editor-panel" data-editor-shell data-testid="editor-shell" aria-labelledby="editor-shell-title">
                <div class="panel-body">
                    <h1 id="editor-shell-title" class="sr-only">${page.t("editor.shell.title")}</h1>
                    <p class="sr-only">${page.t("editor.shell.copy")}</p>

                    <p class="workspace-status" data-workspace-status data-testid="workspace-status" role="status" aria-live="polite" aria-atomic="true" hidden></p>

                    <div class="editor-toolbar" data-testid="editor-toolbar">
                        <div class="editor-toolbar-group">
                            <button type="button" class="editor-button editor-icon-button" data-editor-action="undo" data-testid="editor-undo" title="${page.t("editor.undo")}" aria-label="${page.t("editor.undo")}" disabled><span aria-hidden="true">↶</span></button>
                            <button type="button" class="editor-button editor-icon-button" data-editor-action="redo" data-testid="editor-redo" title="${page.t("editor.redo")}" aria-label="${page.t("editor.redo")}" disabled><span aria-hidden="true">↷</span></button>
                            <button type="button" class="editor-button editor-icon-button" data-editor-action="upload" data-testid="editor-upload" title="${page.t("toolbar.upload")}" aria-label="${page.t("toolbar.upload")}"><span aria-hidden="true">↑</span></button>
                            <input hidden data-document-import-input data-testid="document-import-input" type="file" accept="${page.documentImportAccept()}" data-document-import-labels="${String.join(", ", page.documentImportFormats())}">
                            <button type="button" class="editor-button editor-icon-button" data-editor-action="copy" data-testid="editor-copy" title="${page.t("toolbar.copy")}" aria-label="${page.t("toolbar.copy")}" disabled><span aria-hidden="true">⧉</span></button>
                            <button type="button" class="editor-button editor-icon-button" data-editor-action="download" data-testid="editor-download" title="${page.t("toolbar.download")}" aria-label="${page.t("toolbar.download")}" disabled><span aria-hidden="true">↓</span></button>
                        </div>

                        <div class="editor-counter-bar" aria-live="polite">
                            <button type="button" class="counter-pill counter-button" data-editor-action="stats" data-testid="editor-stats-toggle" aria-expanded="false" aria-controls="text-stats-popover">
                                <strong data-editor-count="characters" data-testid="editor-character-count">0</strong> ${page.t("editor.count.characters")} ·
                                <strong data-editor-count="words" data-testid="editor-word-count">0</strong> ${page.t("editor.count.words")}
                            </button>
                        </div>
                        <span class="sr-only" data-editor-toolbar-status data-testid="editor-toolbar-status" aria-live="polite"></span>
                        <div id="text-stats-popover" class="stats-popover" data-stats-popover data-testid="stats-popover" hidden>
                            <section class="text-stats-panel" data-text-stats-panel data-testid="text-stats-panel" aria-labelledby="text-stats-title">
                                <div class="text-stats-head">
                                    <h3 id="text-stats-title">${page.t("stats.title")}</h3>
                                </div>
                                <dl class="text-stats-grid">
                                    <div class="text-stats-item"><dt>${page.t("stats.characters")}</dt><dd data-text-stats="characters" data-testid="text-stats-characters">0</dd></div>
                                    <div class="text-stats-item"><dt>${page.t("stats.words")}</dt><dd data-text-stats="words" data-testid="text-stats-words">0</dd></div>
                                    <div class="text-stats-item"><dt>${page.t("stats.syllables")}</dt><dd data-text-stats="syllables" data-testid="text-stats-syllables">0</dd></div>
                                    <div class="text-stats-item"><dt>${page.t("stats.sentences")}</dt><dd data-text-stats="sentences" data-testid="text-stats-sentences">0</dd></div>
                                    <div class="text-stats-item"><dt>${page.t("stats.avgSentenceLength")}</dt><dd data-text-stats="avg-sentence-length" data-testid="text-stats-avg-sentence-length">0.0</dd></div>
                                    <div class="text-stats-item"><dt>${page.t("stats.avgSyllablesPerWord")}</dt><dd data-text-stats="avg-syllables-per-word" data-testid="text-stats-avg-syllables-per-word">0.0</dd></div>
                                </dl>
                                <div class="text-stats-flesch" data-text-stats-flesch hidden>
                                    <div class="text-stats-flesch-head"><span>${page.t("stats.flesch.label")}</span><strong data-text-stats="flesch" data-testid="text-stats-flesch">0.0</strong></div>
                                    <div class="text-stats-flesch-bar" role="img" aria-label="${page.t("stats.flesch.ariaLabel")}"><span data-text-stats-flesch-fill></span></div>
                                    <p class="text-stats-flesch-label" data-text-stats-flesch-label data-testid="text-stats-flesch-label">${page.t("stats.flesch.none")}</p>
                                </div>
                            </section>
                        </div>
                    </div>

                    <div class="editor-view" data-editor-view>
                        <div class="editor-canvas" data-testid="editor-canvas" data-editor-canvas data-editor-import-drop-target data-dragging="false">
                            <div class="editor-surface" data-editor-surface data-editor-placeholder="${page.t("editor.placeholder")}"></div>
                            <textarea hidden name="text" data-editor-mirror data-testid="editor-mirror"></textarea>

                            <div class="editor-drop-overlay" aria-hidden="true">
                                <strong>${page.t("import.dropzone.title")}</strong>
                                <span>${page.t("import.dropzone.hint")}</span>
                            </div>

                        </div>
                    </div>

                    <section class="rewrite-diff-panel diff-review" data-review-view data-rewrite-diff-panel data-testid="rewrite-diff-panel" hidden>
                        <header class="diff-review-head">
                            <div>
                                <h2 data-review-title>${page.t("rewrite.diff.title")}</h2>
                                <p data-review-progress></p>
                            </div>
                            <div class="diff-review-actions">
                                <button type="button" class="review-button review-button-icon-only" data-review-mode="inline" aria-pressed="true" aria-label="${page.t("review.inline")}" title="${page.t("review.inline")}">
                                    <svg class="review-button-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                        <circle cx="2.5" cy="4" r=".75" fill="currentColor"/>
                                        <circle cx="2.5" cy="8" r=".75" fill="currentColor"/>
                                        <circle cx="2.5" cy="12" r=".75" fill="currentColor"/>
                                        <path d="M5 4h8M5 8h8M5 12h8" stroke="currentColor" stroke-linecap="round" stroke-width="1.5"/>
                                    </svg>
                                </button>
                                <button type="button" class="review-button review-button-icon-only" data-review-mode="split" aria-pressed="false" aria-label="${page.t("review.split")}" title="${page.t("review.split")}">
                                    <svg class="review-button-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                        <rect x="1.75" y="2.25" width="5.25" height="11.5" rx=".75" stroke="currentColor" stroke-width="1.5"/>
                                        <rect x="9" y="2.25" width="5.25" height="11.5" rx=".75" stroke="currentColor" stroke-width="1.5"/>
                                    </svg>
                                </button>
                                <button type="button" class="review-button review-button-icon-only" data-review-retry aria-label="${page.t("review.retry")}" title="${page.t("review.retry")}">
                                    <svg class="review-button-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                        <path d="M13.5 3.5v3h-3" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"/>
                                        <path d="M13.1 6.5A5.5 5.5 0 1 0 13 9" stroke="currentColor" stroke-linecap="round" stroke-width="1.5"/>
                                    </svg>
                                </button>
                                <button type="button" class="review-button review-button-bulk-reject" data-review-reject-all aria-label="${page.t("review.rejectAll")}" title="${page.t("review.rejectAll")}">
                                    <svg class="review-button-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                        <path d="m3.25 3.25 9.5 9.5m0-9.5-9.5 9.5" stroke="currentColor" stroke-linecap="round" stroke-width="1.75"/>
                                    </svg>
                                    ${page.t("review.rejectAll")}
                                </button>
                                <button type="button" class="review-button review-button-bulk-accept" data-review-accept-all aria-label="${page.t("review.acceptAll")}" title="${page.t("review.acceptAll")}">
                                    <svg class="review-button-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                                        <path d="m2.75 8.25 3.25 3.25 7.25-7.5" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"/>
                                    </svg>
                                    ${page.t("review.acceptAll")}
                                </button>
                            </div>
                        </header>
                        <div class="review-readability" data-review-readability data-testid="review-readability" role="group" aria-label="${page.t("review.readability.label")}" hidden>
                            <span class="review-readability-label">${page.t("review.readability.label")}</span>
                            <span class="review-readability-values">
                                <span class="sr-only">${page.t("review.readability.before")}: </span><strong data-review-readability-before data-testid="review-readability-before">0.0</strong>
                                <span aria-hidden="true">→</span>
                                <span class="sr-only">${page.t("review.readability.after")}: </span><strong data-review-readability-after data-testid="review-readability-after">0.0</strong>
                                <span class="sr-only">${page.t("review.readability.difference")}: </span><span data-review-readability-difference data-testid="review-readability-difference">(±0.0)</span>
                            </span>
                        </div>
                        <p class="review-no-changes" data-review-no-changes hidden>${page.t("review.noChanges")}</p>
                        <div class="diff-inline" data-review-inline data-testid="review-inline"></div>
                        <div class="diff-split" data-review-split data-testid="review-split" hidden>
                            <article>
                                <strong>${page.t("rewrite.diff.before")}</strong>
                                <div data-review-split-before></div>
                            </article>
                            <article>
                                <strong>${page.t("rewrite.diff.after")}</strong>
                                <div data-review-split-after></div>
                            </article>
                        </div>
                    </section>

                    <div class="workspace-busy-overlay" aria-hidden="true">
                        <span class="workspace-spinner"></span>
                        <strong>${page.t("quickAction.status.running")}</strong>
                    </div>

                </div>
            </section>

            <div class="correction-overlay" data-correction-overlay hidden></div>
            <aside class="panel correction-rail" data-correction-rail data-testid="correction-rail" aria-label="${page.t("correction.title")}" aria-hidden="true" hidden>
                <div class="panel-body correction-rail-body">
                    <section class="correction-panel" data-testid="correction-panel" data-correction-panel data-correction-state="idle" role="region" aria-labelledby="correction-panel-title" hidden>
                        <div class="correction-rail-head">
                            <h3 id="correction-panel-title">${page.t("correction.title")}</h3>
                            <button type="button" class="correction-rail-close" data-correction-rail-close aria-label="${page.t("workspace.correction.close")}" title="${page.t("workspace.correction.close")}">×</button>
                        </div>

                        <details class="dictionary-shell" data-testid="dictionary-shell">
                            <summary class="dictionary-head">
                                <strong>${page.t("dictionary.title")}</strong>
                                <span>${page.t("dictionary.subtitle")}</span>
                            </summary>

                            <div class="dictionary-content">
                                <form class="dictionary-form" data-dictionary-form data-testid="dictionary-form">
                                    <label class="sr-only" for="dictionary-input">${page.t("dictionary.input.label")}</label>
                                    <input id="dictionary-input" type="text" class="dictionary-input" data-dictionary-input data-testid="dictionary-input" placeholder="${page.t("dictionary.input.placeholder")}" autocomplete="off" />
                                    <button type="submit" class="dictionary-submit" data-testid="dictionary-submit">${page.t("dictionary.add")}</button>
                                </form>

                                <p class="dictionary-empty" data-dictionary-empty data-testid="dictionary-empty">${page.t("dictionary.empty")}</p>
                                <ul class="dictionary-list" data-dictionary-list data-testid="dictionary-list"></ul>
                            </div>
                        </details>

                        <p class="problem-status" data-correction-status data-testid="correction-status" role="status" aria-live="polite" aria-atomic="true"></p>
                        <div class="problem-list" data-correction-list data-testid="correction-problem-list" role="list" aria-label="${page.t("correction.problemList.ariaLabel")}"></div>

                    </section>
                </div>
            </aside>
        </div>
    </main>
</div>
`)

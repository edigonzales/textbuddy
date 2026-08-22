@import app.textbuddy.web.page.HomePageModel
@import app.textbuddy.document.DocumentOcrLanguage
@param HomePageModel page

@template.layout.main(title = page.title(), lang = page.uiLocaleTag(), skipLinkLabel = page.t("layout.skipLink"), content = @`
<div class="app-shell">
    <header class="app-header" data-testid="shell-header">
        <div class="app-header-inner">
            <span class="app-brand">${page.t("home.header.brand")}</span>
            <nav class="workspace-mode-tabs" role="tablist" aria-label="${page.t("workspace.mode.ariaLabel")}" data-testid="workspace-mode-tabs">
                <button type="button" class="workspace-mode-tab" role="tab" aria-selected="true" data-workspace-mode="transform" data-testid="workspace-mode-transform">${page.t("workspace.mode.transform")}</button>
                <button type="button" class="workspace-mode-tab" role="tab" aria-selected="false" tabindex="-1" data-workspace-mode="validate" data-testid="workspace-mode-validate">
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
            <nav class="locale-switcher" aria-label="${page.t("locale.switcher.ariaLabel")}" data-testid="locale-switcher">
                @for (var language : page.supportedUiLanguages())
                    @if (language.equals(page.uiLocaleLanguage()))
                        <strong aria-current="true">${language.toUpperCase()}</strong>
                    @else
                        <a class="inline-action" href="/?lang=${language}" hreflang="${language}">${language.toUpperCase()}</a>
                    @endif
                @endfor
            </nav>
            </div>
        </div>
    </header>

    <section class="workspace-ribbon" aria-label="${page.t("workspace.ribbon.ariaLabel")}" data-testid="workspace-ribbon">
        <div class="workspace-ribbon-inner" data-workspace-ribbon="transform">
            <div class="ribbon-group">
                <span class="ribbon-group-label">${page.t("workspace.ribbon.structure")}</span>
                <div class="ribbon-action-row">
                    <label class="sr-only" for="mvp-summary-option">${page.t("quickAction.option.summarize.label")}</label>
                    <select id="mvp-summary-option" class="ribbon-select" data-mvp-summary-option data-testid="mvp-summary-option">
                        <option value="sentence">${page.t("quickAction.option.summarize.sentence")}</option>
                        <option value="three_sentence" selected>${page.t("quickAction.option.summarize.threeSentence")}</option>
                        <option value="paragraph">${page.t("quickAction.option.summarize.paragraph")}</option>
                        <option value="page">${page.t("quickAction.option.summarize.page")}</option>
                        <option value="management_summary">Management Summary</option>
                    </select>
                    <button type="button" class="ribbon-action" data-mvp-quick-action="summarize" data-testid="mvp-action-summarize">${page.t("quickAction.action.summarize")}</button>
                </div>
            </div>
            <span class="ribbon-divider" aria-hidden="true"></span>
            <div class="ribbon-group">
                <span class="ribbon-group-label">${page.t("workspace.ribbon.style")}</span>
                <button type="button" class="ribbon-action" data-mvp-quick-action="plain-language" data-testid="mvp-action-plain-language">${page.t("workspace.action.plainLanguage")}</button>
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

    @if (!page.auth().enabled())
        <div class="local-mode-wrap">
            <p class="local-mode-notice" data-testid="auth-status-pill">
                <strong>${page.auth().statusTitle()}</strong>
                <span>${page.auth().statusMessage()}</span>
            </p>
        </div>
    @endif

    <main id="main-content" class="page-shell" tabindex="-1">
        <div
            id="editor-island-root"
            class="editor-root editor-workspace"
            data-testid="editor-island-root"
            data-auth-enabled="${Boolean.toString(page.auth().enabled())}"
            data-authenticated="${Boolean.toString(page.auth().authenticated())}"
            data-auth-login-url="${page.auth().loginUrl()}"
            data-csrf-header="${page.csrfHeaderName()}"
            data-csrf-token="${page.csrfToken()}"
            data-ui-locale="${page.uiLocaleLanguage()}"
            data-ui-messages="${page.uiMessagesJson()}"
            data-inspector-active-tab="actions"
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
                        <div id="text-stats-popover" class="stats-popover" data-stats-popover data-testid="stats-popover" hidden></div>
                    </div>

                    <div class="editor-view" data-editor-view>
                    <div class="editor-canvas" data-testid="editor-canvas" data-editor-canvas data-editor-import-drop-target data-dragging="false">
                        <div class="editor-surface" data-editor-surface data-editor-placeholder="${page.t("editor.placeholder")}"></div>
                        <textarea hidden name="text" data-editor-mirror data-testid="editor-mirror"></textarea>

                        <div class="editor-drop-overlay" aria-hidden="true">
                            <strong>${page.t("import.dropzone.title")}</strong>
                            <span>${page.t("import.dropzone.hint")}</span>
                        </div>

                        <div class="rewrite-bubble" data-rewrite-bubble data-testid="rewrite-bubble" role="dialog" aria-modal="false" hidden>
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
                    </div>

                    <section class="rewrite-diff-panel diff-review" data-review-view data-rewrite-diff-panel data-testid="rewrite-diff-panel" hidden>
                        <header class="diff-review-head">
                            <div>
                                <h2 data-review-title>${page.t("rewrite.diff.title")}</h2>
                                <p data-review-progress></p>
                            </div>
                            <div class="diff-review-actions">
                                <button type="button" class="review-button" data-review-mode="inline" aria-pressed="true">${page.t("review.inline")}</button>
                                <button type="button" class="review-button" data-review-mode="split" aria-pressed="false">${page.t("review.split")}</button>
                                <button type="button" class="review-button" data-review-retry>${page.t("review.retry")}</button>
                                <button type="button" class="review-button" data-review-reject-all>${page.t("review.rejectAll")}</button>
                                <button type="button" class="review-button review-button-primary" data-review-accept-all>${page.t("review.acceptAll")}</button>
                                <button type="button" class="review-button" data-rewrite-diff-undo data-testid="rewrite-diff-undo">${page.t("review.cancel")}</button>
                            </div>
                        </header>
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
                        <div hidden aria-hidden="true">
                            <div data-rewrite-diff-before data-testid="rewrite-diff-before"></div>
                            <div data-rewrite-diff-after data-testid="rewrite-diff-after"></div>
                        </div>
                    </section>

                    <div class="workspace-busy-overlay" aria-hidden="true">
                        <span class="workspace-spinner"></span>
                        <strong>${page.t("quickAction.status.running")}</strong>
                    </div>

                </div>
            </section>

            <div class="correction-overlay" data-correction-overlay hidden></div>
            <aside class="panel inspector-panel correction-rail" data-correction-rail data-testid="inspector-panel" aria-label="${page.t("correction.title")}" aria-hidden="true" hidden>
                <div class="panel-body inspector-body">
                    <div class="inspector-head" data-compatibility-host hidden inert>
                        <div class="inspector-tabs" role="tablist" aria-label="${page.t("inspector.tabs.ariaLabel")}">
                            <button type="button" id="inspector-tab-correction" class="inspector-tab" role="tab" aria-controls="inspector-panel-correction" aria-selected="false" data-inspector-tab="correction" data-testid="inspector-tab-correction">${page.t("inspector.tab.correction")}</button>
                            <button type="button" id="inspector-tab-actions" class="inspector-tab" role="tab" aria-controls="inspector-panel-actions" aria-selected="true" data-inspector-tab="actions" data-testid="inspector-tab-actions">${page.t("inspector.tab.actions")}</button>
                            <button type="button" id="inspector-tab-advisor" class="inspector-tab" role="tab" aria-controls="inspector-panel-advisor" aria-selected="false" data-inspector-tab="advisor" data-testid="inspector-tab-advisor">${page.t("inspector.tab.advisor")}</button>
                            <button type="button" id="inspector-tab-import" class="inspector-tab" role="tab" aria-controls="inspector-panel-import" aria-selected="false" data-inspector-tab="import" data-testid="inspector-tab-import">${page.t("inspector.tab.import")}</button>
                            <button type="button" id="inspector-tab-stats" class="inspector-tab" role="tab" aria-controls="inspector-panel-stats" aria-selected="false" data-inspector-tab="stats" data-testid="inspector-tab-stats">${page.t("inspector.tab.stats")}</button>
                        </div>
                    </div>

                    <section id="inspector-panel-actions" class="inspector-section quick-action-panel" data-inspector-panel="actions" data-quick-action-panel data-testid="quick-action-panel" data-quick-action-state="idle" data-quick-action-selected-action="plain-language" role="tabpanel" aria-labelledby="inspector-tab-actions" data-compatibility-host hidden inert>
                        <div class="quick-action-panel-head">
                            <p class="quick-action-status" data-quick-action-status data-testid="quick-action-status" role="status" aria-live="polite" aria-atomic="true"></p>
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
                                    <h4 data-quick-action-active-label data-testid="quick-action-active-label">${page.t("quickAction.action.plainLanguage")}</h4>
                                </div>

                                <div class="quick-action-configs">
                                    <div class="quick-action-config" data-quick-action-config="plain-language" data-testid="quick-action-config-plain-language">
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="proofread" data-testid="quick-action-config-proofread" hidden>
                                    </div>
                                    <div class="quick-action-config" data-quick-action-config="bullet-points" data-testid="quick-action-config-bullet-points" hidden>
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

                    <section id="inspector-panel-correction" class="inspector-section correction-panel" data-inspector-panel="correction" data-testid="correction-panel" data-correction-panel data-correction-state="idle" role="region" aria-labelledby="correction-panel-title" hidden>
                        <div class="correction-rail-head">
                            <h3 id="correction-panel-title">${page.t("correction.title")}</h3>
                            <button type="button" class="correction-rail-close" data-correction-rail-close aria-label="${page.t("workspace.correction.close")}" title="${page.t("workspace.correction.close")}">×</button>
                        </div>

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
                        </div>

                        <p class="problem-status" data-correction-status data-testid="correction-status" role="status" aria-live="polite" aria-atomic="true"></p>
                        <div class="problem-list" data-correction-list data-testid="correction-problem-list" role="list" aria-label="${page.t("correction.problemList.ariaLabel")}"></div>

                    </section>

                    <section id="inspector-panel-advisor" class="inspector-section advisor-panel" data-inspector-panel="advisor" data-testid="advisor-panel" data-advisor-panel data-advisor-state="idle" role="tabpanel" aria-labelledby="inspector-tab-advisor" data-compatibility-host hidden inert>
                        <h3 id="advisor-panel-title">${page.t("advisor.title")}</h3>

                        <div class="advisor-toolbar">
                            <p class="advisor-status" data-advisor-status data-testid="advisor-status" role="status" aria-live="polite" aria-atomic="true">
                                ${page.advisorDocs().size()} ${page.t("advisor.docsAvailable")}
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
                                        <button type="button" class="advisor-link" data-advisor-open data-advisor-doc-url="${advisorDoc.documentUrl()}" data-advisor-doc-title="${advisorDoc.title()}" data-advisor-doc-name="${advisorDoc.name()}" data-testid="advisor-doc-open">${page.t("advisor.openViewer")}</button>
                                        <a class="advisor-link" href="${advisorDoc.documentUrl()}" target="_blank" rel="noopener noreferrer" data-testid="advisor-doc-open-tab">${page.t("advisor.openTab")}</a>
                                        <a class="advisor-link advisor-link-secondary" href="${advisorDoc.documentUrl()}" download="${advisorDoc.name()}.pdf" data-testid="advisor-doc-download">${page.t("advisor.downloadPdf")}</a>
                                    </div>
                                </article>
                            @endfor
                        </div>

                        <section class="advisor-results-panel" data-testid="advisor-results-panel" aria-labelledby="advisor-results-title">
                            <div class="advisor-results-head">
                                <div>
                                    <h3 id="advisor-results-title">${page.t("advisor.results.title")}</h3>
                                    <p class="panel-copy">${page.t("advisor.results.copy")}</p>
                                </div>

                                <span class="advisor-result-counter" data-advisor-result-count data-testid="advisor-result-count">0 ${page.t("advisor.results.hits")}</span>
                            </div>

                            <p class="advisor-results-empty" data-advisor-result-empty data-testid="advisor-result-empty">${page.t("advisor.results.empty")}</p>
                            <div class="advisor-results" data-advisor-result-list data-testid="advisor-result-list" role="list" aria-label="${page.t("advisor.results.ariaLabel")}"></div>

                            <section class="advisor-detail" data-advisor-result-detail data-testid="advisor-result-detail" role="region" aria-live="polite" aria-atomic="true" aria-labelledby="advisor-result-detail-title" hidden>
                                <div class="advisor-detail-head">
                                    <h3 id="advisor-result-detail-title" data-advisor-result-detail-title data-testid="advisor-result-detail-title"></h3>

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
                                <h3 id="advisor-pdf-title" data-advisor-pdf-title data-testid="advisor-pdf-title">${page.t("viewer.title")}</h3>

                                <div class="advisor-detail-actions">
                                    <a class="advisor-link" href="#" data-advisor-pdf-download data-testid="advisor-pdf-download">${page.t("viewer.download")}</a>
                                    <button type="button" class="advisor-link advisor-link-secondary" data-advisor-pdf-close data-testid="advisor-pdf-close">${page.t("viewer.close")}</button>
                                </div>
                            </div>

                            <iframe class="advisor-pdf-frame" data-advisor-pdf-frame data-testid="advisor-pdf-frame" title="${page.t("viewer.frameTitle")}" loading="lazy"></iframe>
                        </section>

                    </section>

                    <section id="inspector-panel-import" class="inspector-section document-import-panel" data-inspector-panel="import" data-document-import-panel data-document-import-state="idle" data-document-import-labels="${String.join(", ", page.documentImportFormats())}" data-testid="document-import-panel" role="tabpanel" aria-labelledby="inspector-tab-import" data-compatibility-host hidden inert>
                        <h3 id="document-import-title">${page.t("import.title")}</h3>
                        <p class="document-import-status" data-document-import-status data-testid="document-import-status" role="status" aria-live="polite" aria-atomic="true"></p>

                        <div class="document-import-body">
                            <div class="document-import-dropzone" data-document-import-dropzone data-dragging="false" data-busy="false" data-testid="document-import-dropzone" aria-label="${page.t("import.dropzone.ariaLabel")}" aria-describedby="document-import-dropzone-hint" aria-disabled="false" aria-busy="false">
                                <input id="document-import-input" class="document-import-input" data-document-import-input data-testid="document-import-input" type="file" accept="${page.documentImportAccept()}" aria-hidden="true" tabindex="-1">
                                <strong>${page.t("import.dropzone.title")}</strong>
                                <span id="document-import-dropzone-hint">${page.t("import.dropzone.hint")}</span>
                                <label class="quick-action-select-shell document-import-select-shell" for="document-import-ocr-language">
                                    <span class="quick-action-select-label">${page.t("import.ocr.label")}</span>
                                    <select id="document-import-ocr-language" class="quick-action-select document-import-ocr-language" data-document-import-ocr-language data-testid="document-import-ocr-language">
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

                    <section id="inspector-panel-stats" class="inspector-section text-stats-panel" data-inspector-panel="stats" data-text-stats-panel data-testid="text-stats-panel" role="tabpanel" aria-labelledby="inspector-tab-stats" data-compatibility-host hidden inert>
                        <div class="text-stats-head">
                            <div>
                                <h3>${page.t("stats.title")}</h3>
                            </div>
                        </div>

                        <dl class="text-stats-grid">
                            <div class="text-stats-item">
                                <dt>${page.t("stats.characters")}</dt>
                                <dd data-text-stats="characters" data-testid="text-stats-characters">0</dd>
                            </div>
                            <div class="text-stats-item">
                                <dt>${page.t("stats.words")}</dt>
                                <dd data-text-stats="words" data-testid="text-stats-words">0</dd>
                            </div>
                            <div class="text-stats-item">
                                <dt>${page.t("stats.syllables")}</dt>
                                <dd data-text-stats="syllables" data-testid="text-stats-syllables">0</dd>
                            </div>
                            <div class="text-stats-item">
                                <dt>${page.t("stats.sentences")}</dt>
                                <dd data-text-stats="sentences" data-testid="text-stats-sentences">0</dd>
                            </div>
                            <div class="text-stats-item">
                                <dt>${page.t("stats.avgSentenceLength")}</dt>
                                <dd data-text-stats="avg-sentence-length" data-testid="text-stats-avg-sentence-length">0.0</dd>
                            </div>
                            <div class="text-stats-item">
                                <dt>${page.t("stats.avgSyllablesPerWord")}</dt>
                                <dd data-text-stats="avg-syllables-per-word" data-testid="text-stats-avg-syllables-per-word">0.0</dd>
                            </div>
                        </dl>

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
                </div>
            </aside>
        </div>
    </main>
</div>
`)

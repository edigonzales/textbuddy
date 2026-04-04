@import app.textbuddy.web.error.ErrorPageModel
@param ErrorPageModel page

@template.layout.main(title = page.title(), content = @`
<div class="page-shell error-shell">
    <section class="panel error-card" data-testid="error-page">
        <div class="panel-body">
            <p class="eyebrow">Fehlerbild</p>
            <h1>${page.statusCode()} ${page.errorTitle()}</h1>
            <p class="panel-copy">${page.message()}</p>

            <div class="stack-list">
                <div class="stack-item">
                    <strong>Pfad</strong>
                    <code>${page.path()}</code>
                </div>
                <div class="stack-item">
                    <strong>Trace-ID</strong>
                    <code>${page.traceId()}</code>
                </div>
            </div>

            <div class="error-actions">
                <a class="inline-action" href="/">Zur Startseite</a>
            </div>
        </div>
    </section>
</div>
`)

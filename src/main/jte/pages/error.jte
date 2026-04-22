@import app.textbuddy.web.error.ErrorPageModel
@param ErrorPageModel page

@template.layout.main(title = page.title(), lang = page.lang(), skipLinkLabel = page.skipLinkLabel(), content = @`
<main id="main-content" class="page-shell error-shell" tabindex="-1">
    <section class="panel error-card" data-testid="error-page">
        <div class="panel-body">
            <p class="eyebrow">${page.eyebrow()}</p>
            <h1>${page.statusCode()} ${page.errorTitle()}</h1>
            <p class="panel-copy">${page.message()}</p>

            <div class="stack-list">
                <div class="stack-item">
                    <strong>${page.pathLabel()}</strong>
                    <code>${page.path()}</code>
                </div>
                <div class="stack-item">
                    <strong>${page.traceLabel()}</strong>
                    <code>${page.traceId()}</code>
                </div>
            </div>

            <div class="error-actions">
                <a class="inline-action" href="/">${page.backHomeLabel()}</a>
            </div>
        </div>
    </section>
</main>
`)

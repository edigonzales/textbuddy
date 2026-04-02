@import gg.jte.Content
@param String title
@param Content content
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <link rel="stylesheet" href="/styles/app.css">
    <script src="/webjars/htmx.org/2.0.8/dist/htmx.min.js" defer></script>
</head>
<body>
${content}
</body>
</html>

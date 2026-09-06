#!/usr/bin/env python3
"""Prepare the pinned German wordhoard data for the isolated Java experiment."""

from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import io
import json
import shutil
import tempfile
import urllib.request
import zipfile
from pathlib import Path


WORDHOARD_RELEASE = "v0.1.0"
WORDHOARD_COMMIT = "1bc5730e8d6e682c416c03680b7cb8c6c7ca8cd0"
ARCHIVE_URL = (
    "https://github.com/natema/wordhoard/releases/download/"
    f"{WORDHOARD_RELEASE}/wordhoard-csv-{WORDHOARD_RELEASE}.zip"
)
ARCHIVE_SHA256 = "83837efd46241e7226fc6daaa9d0cc81b57bf746434b8c539049c660d98ba761"
CSV_NAME = "wordhoard-de.csv"
LICENSE_URL = f"https://raw.githubusercontent.com/natema/wordhoard/{WORDHOARD_COMMIT}/LICENSE"
NOTICE_URL = f"https://raw.githubusercontent.com/natema/wordhoard/{WORDHOARD_COMMIT}/NOTICE.md"
OUTPUT_COLUMNS = (
    "lemma",
    "pos",
    "frequency_rank",
    "frequency_count",
    "cefr_estimate",
    "cefr_source",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Download, verify and reduce the pinned German wordhoard CSV."
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).parents[2]
        / "src"
        / "test"
        / "resources"
        / "wordhoard-experiment",
    )
    return parser.parse_args()


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "textbuddy-wordhoard-experiment"})
    with urllib.request.urlopen(request) as response:
        return response.read()


def normalized_text_resource(url: str) -> bytes:
    return download(url).rstrip(b"\r\n") + b"\n"


def cefr_source(notes: str) -> str:
    for item in notes.split(";"):
        item = item.strip()
        if item.startswith("cefr:"):
            return item.removeprefix("cefr:")
    return "unknown"


def main() -> None:
    args = parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)

    archive = download(ARCHIVE_URL)
    archive_hash = sha256(archive)
    if archive_hash != ARCHIVE_SHA256:
        raise SystemExit(
            f"Expected archive SHA-256 {ARCHIVE_SHA256}, found {archive_hash}."
        )

    rows: list[dict[str, str]] = []
    with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
        with bundle.open(CSV_NAME) as raw_csv:
            reader = csv.DictReader(io.TextIOWrapper(raw_csv, encoding="utf-8-sig"))
            for source in reader:
                rows.append(
                    {
                        "lemma": source["lemma"],
                        "pos": source["pos"],
                        "frequency_rank": source["frequency_rank"],
                        "frequency_count": source["frequency_count"],
                        "cefr_estimate": source["cefr_estimate"],
                        "cefr_source": cefr_source(source.get("notes", "")),
                    }
                )

    rows.sort(key=lambda row: (int(row["frequency_rank"]), row["lemma"], row["pos"]))
    with tempfile.TemporaryDirectory() as temporary_directory:
        temporary = Path(temporary_directory) / "lexicon.tsv.gz"
        with temporary.open("wb") as raw_output:
            with gzip.GzipFile(filename="", mode="wb", fileobj=raw_output, mtime=0) as compressed:
                with io.TextIOWrapper(compressed, encoding="utf-8", newline="") as text_output:
                    writer = csv.DictWriter(
                        text_output,
                        fieldnames=OUTPUT_COLUMNS,
                        delimiter="\t",
                        lineterminator="\n",
                    )
                    writer.writeheader()
                    writer.writerows(rows)
        shutil.copyfile(temporary, output / "lexicon.tsv.gz")

    lexicon_bytes = (output / "lexicon.tsv.gz").read_bytes()
    levels: dict[str, int] = {}
    for row in rows:
        level = row["cefr_estimate"]
        levels[level] = levels.get(level, 0) + 1

    manifest = {
        "schemaVersion": 1,
        "source": {
            "repository": "https://github.com/natema/wordhoard",
            "release": WORDHOARD_RELEASE,
            "commit": WORDHOARD_COMMIT,
            "archiveUrl": ARCHIVE_URL,
            "archiveSha256": ARCHIVE_SHA256,
            "csvName": CSV_NAME,
            "dataLicense": "CC-BY-SA-4.0",
        },
        "transformation": {
            "description": "German rows reduced to lexical difficulty fields; forms and gender omitted.",
            "rowCount": len(rows),
            "columns": list(OUTPUT_COLUMNS),
            "lexiconSha256": sha256(lexicon_bytes),
            "cefrCounts": dict(sorted(levels.items())),
        },
    }
    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (output / "LICENSE-wordhoard-data.txt").write_bytes(normalized_text_resource(LICENSE_URL))
    (output / "NOTICE-wordhoard.txt").write_bytes(normalized_text_resource(NOTICE_URL))


if __name__ == "__main__":
    main()

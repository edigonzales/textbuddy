#!/usr/bin/env python3
"""Export pinned ZIX reference data for the Java compatibility experiment."""

from __future__ import annotations

import argparse
import csv
import importlib.util
import json
import subprocess
import sys
import tomllib
from pathlib import Path
from typing import Any


PINNED_ZIX_COMMIT = "3cd7e7e9fd0937e1c41e2bf0e040950172ab3a6e"
FEATURE_NAMES = [
    "sentence_length_mean",
    "rix",
    "vocab_a1",
    "vocab_a2",
    "vocab_b1",
    "common_word_score",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate ZIX golden values and Java-friendly derived resources."
    )
    parser.add_argument(
        "--zix-checkout",
        required=True,
        type=Path,
        help="Checkout of the official ZIX repository at the pinned commit.",
    )
    parser.add_argument(
        "--corpus",
        type=Path,
        default=Path(__file__).with_name("corpus.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).parents[2]
        / "src"
        / "test"
        / "resources"
        / "zix-compatibility",
    )
    return parser.parse_args()


def git_head(checkout: Path) -> str:
    return subprocess.run(
        ["git", "-C", str(checkout), "rev-parse", "HEAD"],
        check=True,
        capture_output=True,
        text=True,
    ).stdout.strip()


def load_official_module(checkout: Path):
    sys.path.insert(0, str(checkout))
    module_path = checkout / "zix" / "understandability.py"
    spec = importlib.util.spec_from_file_location("zix.understandability", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load official ZIX module from {module_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def plain_list(value: Any) -> list[float]:
    if hasattr(value, "tolist"):
        value = value.tolist()
    if not isinstance(value, list):
        value = [value]
    return [float(item) for item in value]


def write_tsv(path: Path, headers: list[str], rows: list[list[Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(headers)
        writer.writerows(rows)


def main() -> None:
    args = parse_args()
    checkout = args.zix_checkout.resolve()
    commit = git_head(checkout)
    if commit != PINNED_ZIX_COMMIT:
        raise SystemExit(
            f"Expected ZIX commit {PINNED_ZIX_COMMIT}, found {commit}."
        )
    if not (checkout / "uv.lock").is_file():
        raise SystemExit("The pinned checkout has no uv.lock file.")

    project = tomllib.loads((checkout / "pyproject.toml").read_text(encoding="utf-8"))
    zix_version = project["project"]["version"]
    official = load_official_module(checkout)
    corpus = json.loads(args.corpus.read_text(encoding="utf-8"))

    model_version = official.nlp_pipeline.meta.get("version", "unknown")
    cases = []
    for case in corpus["cases"]:
        normalized_text = official._punctuate_lines(case["text"])
        feature_frame = official._extract_features(normalized_text)
        if feature_frame.isnull().values.any():
            raise ValueError(f"ZIX returned empty features for {case['id']}")
        features = {
            name: float(feature_frame.iloc[0][name]) for name in FEATURE_NAMES
        }
        score = float(official._calculate_score(feature_frame))
        cases.append(
            {
                **case,
                "wordCount": len(case["text"].split()),
                "normalizedText": normalized_text,
                "features": features,
                "zixScore": score,
                "cefrBand": official.get_cefr(score),
            }
        )

    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    reference = {
        "schemaVersion": 1,
        "source": {
            "repository": "https://github.com/machinelearningZH/zix_understandability-index",
            "commit": commit,
            "zixVersion": zix_version,
            "spacyModel": "de_core_news_sm",
            "spacyModelVersion": model_version,
            "lockFile": "uv.lock",
        },
        "featureOrder": FEATURE_NAMES,
        "cases": cases,
    }
    (output / "reference.json").write_text(
        json.dumps(reference, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    cefr_rows = (
        official.cefr_vocab[["lemma_ch", "level"]]
        .dropna()
        .drop_duplicates()
        .sort_values(["lemma_ch", "level"])
        .values.tolist()
    )
    write_tsv(output / "cefr-vocabulary.tsv", ["lemma", "level"], cefr_rows)

    common_rows = sorted(
        [[str(lemma), float(score)] for lemma, score in official.word_scores.items()],
        key=lambda row: row[0],
    )
    write_tsv(output / "common-word-scores.tsv", ["lemma", "score"], common_rows)

    scaler = official.scaler
    regressor = official.clf
    model = {
        "schemaVersion": 1,
        "featureOrder": FEATURE_NAMES,
        "scalerMean": plain_list(scaler.mean_),
        "scalerScale": plain_list(scaler.scale_),
        "ridgeCoefficients": plain_list(regressor.coef_),
        "ridgeIntercept": float(regressor.intercept_),
        "scoreTransform": {
            "predictionOffset": 1.0,
            "multiplier": 2.0,
            "shift": 5.5,
            "minimum": -10.0,
            "maximum": 10.0,
        },
        "sourceCommit": commit,
        "zixVersion": zix_version,
    }
    (output / "model.json").write_text(
        json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    print(f"Generated {len(cases)} reference cases in {output}")


if __name__ == "__main__":
    main()

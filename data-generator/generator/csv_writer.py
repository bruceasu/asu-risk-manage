"""Streaming CSV writer."""

from __future__ import annotations

import csv
from pathlib import Path
from typing import Iterable

from generator.schema_validation import validate_row


class ChunkedCsvWriter:
    """Writes rows incrementally and flushes regularly."""

    def __init__(self, path: Path, fieldnames: list[str], chunk_size: int, validate: bool = True) -> None:
        self.path = path
        self.fieldnames = fieldnames
        self.chunk_size = max(1, chunk_size)
        self.validate = validate
        self._buffered = 0
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._handle = self.path.open("w", newline="", encoding="utf-8")
        self._writer = csv.DictWriter(self._handle, fieldnames=self.fieldnames)
        self._writer.writeheader()

    def write_row(self, row: dict[str, object]) -> None:
        if self.validate:
            validate_row(self.path.name, row)
        self._writer.writerow(row)
        self._buffered += 1
        if self._buffered >= self.chunk_size:
            self._handle.flush()
            self._buffered = 0

    def write_rows(self, rows: Iterable[dict[str, object]]) -> None:
        for row in rows:
            self.write_row(row)

    def close(self) -> None:
        self._handle.flush()
        self._handle.close()

    def __enter__(self) -> "ChunkedCsvWriter":
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()

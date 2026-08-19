#!/usr/bin/env python3
"""Extract the embedded FlaxClient JAR from a self-contained launcher EXE.

The Rust launcher embeds the JAR as raw ZIP/JAR bytes via include_bytes!().
This script walks ZIP EOCD records inside the executable, reconstructs each
embedded ZIP range, and selects the Java archive with the strongest FlaxClient
signature.
"""

from __future__ import annotations

import hashlib
import io
import struct
import sys
import zipfile
from pathlib import Path

EOCD = b"PK\x05\x06"
LOCAL_FILE = b"PK\x03\x04"


def iter_embedded_zips(data: bytes):
    cursor = 0
    while True:
        eocd = data.find(EOCD, cursor)
        if eocd < 0:
            return
        cursor = eocd + 1

        if eocd + 22 > len(data):
            continue

        try:
            (
                disk_no,
                cd_disk,
                entries_disk,
                entries_total,
                cd_size,
                cd_offset,
                comment_len,
            ) = struct.unpack_from("<HHHHIIH", data, eocd + 4)
        except struct.error:
            continue

        if disk_no != 0 or cd_disk != 0 or entries_disk != entries_total:
            continue

        archive_end = eocd + 22 + comment_len
        if archive_end > len(data):
            continue

        absolute_cd_start = eocd - cd_size
        archive_start = absolute_cd_start - cd_offset
        if archive_start < 0 or archive_start >= absolute_cd_start:
            continue
        if data[archive_start : archive_start + 4] != LOCAL_FILE:
            continue

        blob = data[archive_start:archive_end]
        try:
            with zipfile.ZipFile(io.BytesIO(blob)) as zf:
                bad = zf.testzip()
                if bad is not None:
                    continue
                names = zf.namelist()
        except (OSError, zipfile.BadZipFile, RuntimeError):
            continue

        yield archive_start, archive_end, blob, names


def score_flax_jar(names: list[str]) -> tuple[int, int]:
    lowered = [name.lower() for name in names]
    class_count = sum(name.endswith(".class") for name in lowered)
    score = 0

    if "meta-inf/manifest.mf" in lowered:
        score += 100
    if class_count:
        score += min(class_count, 1000)
    if any(name.startswith("me/eldodebug/") for name in lowered):
        score += 5000
    if any("flaxclient" in name for name in lowered):
        score += 2500
    if any(name.endswith("mcmod.info") for name in lowered):
        score += 250

    return score, class_count


def main() -> int:
    if len(sys.argv) != 3:
        print(f"usage: {Path(sys.argv[0]).name} <launcher.exe> <FlaxClient-Release.jar>", file=sys.stderr)
        return 2

    source = Path(sys.argv[1])
    output = Path(sys.argv[2])
    data = source.read_bytes()
    if not data.startswith(b"MZ"):
        raise SystemExit(f"input is not a Windows PE executable: {source}")

    candidates = []
    for start, end, blob, names in iter_embedded_zips(data):
        score, class_count = score_flax_jar(names)
        candidates.append((score, class_count, start, end, blob, names))

    if not candidates:
        raise SystemExit("no embedded ZIP/JAR archives were found in the launcher")

    candidates.sort(key=lambda item: (item[0], item[1], len(item[4])), reverse=True)
    score, class_count, start, end, blob, names = candidates[0]

    if score < 100 or class_count == 0:
        summary = ", ".join(
            f"score={item[0]} classes={item[1]} bytes={len(item[4])}" for item in candidates[:5]
        )
        raise SystemExit(f"embedded archives were found, but none looked like a Java client JAR: {summary}")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(blob)

    sha256 = hashlib.sha256(blob).hexdigest()
    print(f"extracted JAR: {output}")
    print(f"launcher range: 0x{start:x}-0x{end:x}")
    print(f"size: {len(blob)} bytes")
    print(f"class files: {class_count}")
    print(f"selection score: {score}")
    print(f"sha256: {sha256}")
    print(f"entries: {len(names)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

import os
from pathlib import Path

KEEP_MAP = {
    2: 0,
    4: 1,
}
NEW_NAMES = ["cracked", "healthy"]

SPLITS = ["train", "valid", "test"]

def filter_labels_dir(labels_dir: Path):
    kept_files = 0
    dropped_lines = 0
    kept_lines = 0
    for label_file in labels_dir.glob("*.txt"):
        lines = label_file.read_text().splitlines()
        new_lines = []
        for line in lines:
            if not line.strip():
                continue
            parts = line.split()
            old_cls = int(parts[0])
            if old_cls in KEEP_MAP:
                parts[0] = str(KEEP_MAP[old_cls])
                new_lines.append(" ".join(parts))
                kept_lines += 1
            else:
                dropped_lines += 1
        label_file.write_text("\n".join(new_lines) + ("\n" if new_lines else ""))
        kept_files += 1
    return kept_files, kept_lines, dropped_lines


def main():
    root = Path(__file__).parent
    total_kept = total_dropped = 0
    for split in SPLITS:
        labels_dir = root / split / "labels"
        if not labels_dir.exists():
            print(f"  (lipseste {labels_dir}, sar peste)")
            continue
        files, kept, dropped = filter_labels_dir(labels_dir)
        print(f"{split}: {files} fisiere procesate, {kept} boxe pastrate, {dropped} boxe eliminate")
        total_kept += kept
        total_dropped += dropped

    data_yaml = root / "data.yaml"
    if data_yaml.exists():
        content = data_yaml.read_text()
        lines = content.splitlines()
        out = []
        for line in lines:
            if line.startswith("nc:"):
                out.append(f"nc: {len(NEW_NAMES)}")
            elif line.startswith("names:"):
                out.append(f"names: {NEW_NAMES}")
            else:
                out.append(line)
        data_yaml.write_text("\n".join(out) + "\n")
        print(f"data.yaml actualizat: nc={len(NEW_NAMES)}, names={NEW_NAMES}")

    print(f"\nTotal: {total_kept} boxe pastrate, {total_dropped} boxe eliminate.")


if __name__ == "__main__":
    main()



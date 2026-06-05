#!/usr/bin/env python3
import os
import zipfile
from pathlib import Path


def main():
    project_root = Path(__file__).resolve().parent
    zip_name = f"{project_root.name}.zip"
    zip_path = project_root / zip_name

    if zip_path.exists():
        print(f"Removing existing ZIP: {zip_path}")
        zip_path.unlink()

    include_items = [
        "platformio.ini",
        "src",
        "lib",
        "include",
    ]

    print(f"Project folder: {project_root}")
    print(f"Creating ZIP:   {zip_path}")

    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for item in include_items:
            path = project_root / item
            if not path.exists():
                print(f"Skipping (not found): {item}")
                continue

            if path.is_file():
                arcname = path.name
                print(f"Adding file: {arcname}")
                zf.write(path, arcname=arcname)
            else:
                for root, dirs, files in os.walk(path):
                    root_path = Path(root)
                    rel_root = root_path.relative_to(project_root)
                    for file in files:
                        file_path = root_path / file
                        arcname = rel_root / file
                        print(f"Adding file: {arcname}")
                        zf.write(file_path, arcname=str(arcname))

    print("Zipped")


if __name__ == "__main__":
    main()

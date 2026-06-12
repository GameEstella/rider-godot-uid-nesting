# Godot Sidecar File Nesting

This Rider plugin nests Godot sidecar files under their matching files in the tree view.

Examples:

- `lane.mp4.uid` is shown under `lane.mp4`
- `lv1.txt.uid` is shown under `lv1.txt`
- `main.gd.uid` is shown under `main.gd`
- `tutorial.en.translation` is shown under `tutorial.csv`
- `tutorial.ja.translation` is shown under `tutorial.csv`

The nesting logic only activates when the opened project root contains a `project.godot` file.

## Build

Run:

```bash
./gradlew build
```

## Run in Rider sandbox

Run:

```bash
./gradlew runIde
```

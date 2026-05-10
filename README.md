# BPlayer

Minimal Android audio player for folders of mp3/m4b/flac. Built around two ideas:

- **Per-book resume.** Each folder (or single audio file at the top level) remembers
  which track you were on and where in that track. Switch between books or albums
  freely — each one resumes from its own bookmark.
- **Android Auto.** Exposes the same folder browse tree to your car's head unit.

Other things it does:

- Tree-based browser: pick any root folder via the system folder picker; subfolders
  with subfolders drill in, subfolders with audio files play, single files play directly.
- Natural numeric sort for filenames (e.g. chapter `01.mp3 … 10.mp3` instead of
  `01, 10, 02`).
- Cover art picked from `Cover.jpg` / `Front.jpg` / `Folder.jpg` / first jpg in folder.
- Per-row delete (recursive for folders) and reset-progress actions.
- Full-screen Now Playing with draggable scrubber.

## Build

Requires the Android SDK and a JDK 17+ (Android Studio's bundled JDK is fine).

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install with
`adb install -r <apk>`.

Min SDK 34 (Android 14), targets compileSdk 36.

## Android Auto

Sideloaded media apps don't show up in Android Auto by default. One-time setup on
the phone:

1. Open the **Android Auto** app, tap the version number ~10 times to enable
   developer mode.
2. In Developer settings, enable **Unknown sources**.

BPlayer then appears in the car's media app picker.

## Pre-commit hook

The repo ships with a pre-commit guard at `.githooks/pre-commit` that blocks
commits containing secrets, absolute `/Users/<name>/` paths, or non-noreply
email addresses. It runs `gitleaks` plus a small set of custom regex checks.

After cloning, enable it once:

```sh
brew install gitleaks   # one-time, if not installed
git config core.hooksPath .githooks
```

If a match is intentional, bypass with `git commit --no-verify`.

## Setting up on a new machine

The git repo doesn't carry the upload keystore or its password — both are
gitignored. After cloning to a new machine:

```sh
git clone git@github.com:skranin/bplayer.git
cd bplayer
git config core.hooksPath .githooks            # re-enable pre-commit hook
# copy upload-keystore.jks from your backup → project root
# copy or recreate keystore.properties → project root
```

`./gradlew :app:bundleRelease` then produces a signed AAB as usual.

## License

MIT — see [LICENSE](LICENSE). Free to use, modify, and redistribute.

## How resume works

Bookmarks are keyed by the book's folder URI (or file URI for a loose `.m4b`).
Saves happen on pause, on track transition, every 10 seconds during playback,
and synchronously right before the queue is replaced when you tap a different
book — so the outgoing book's position lands before the new one starts.

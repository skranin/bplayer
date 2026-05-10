# Play Store assets

Everything you'd paste into the Play Console for BPlayer. None of this is
shipped with the app — the directory exists for convenience and is ignored
by the build.

| File                     | What it is                                      |
| ------------------------ | ----------------------------------------------- |
| `icon-512.png`           | High-res app icon, exactly what Play wants.     |
| `listing.md`             | Listing copy: name, short + full description.   |
| `play-console-forms.md`  | Pre-baked answers for data safety, ratings, etc. |

You still have to produce yourself:

- **Feature graphic** — 1024 × 500 PNG/JPG, no transparency. Suggested:
  red play disc on left third over a white background, "BPlayer" wordmark
  to the right in a clean sans-serif. Any image tool works.
- **Phone screenshots** — at least 2, ideally 4–8. From the running app on
  your S24+ or Z Fold 7. Recommended shots:
  1. Browser at the root (folder list).
  2. Browser inside a Books folder showing book tiles.
  3. Now Playing screen with cover, title, scrubber visible.
  4. Optional: Android Auto screenshot if you can capture one.

For phone screenshots, with the device connected:
```
adb shell screencap -p /sdcard/bplayer-1.png && adb pull /sdcard/bplayer-1.png .
```

The privacy policy is at the repo root: `PRIVACY.md`. Once committed and
pushed, its public URL is:
`https://github.com/skranin/bplayer/blob/master/PRIVACY.md`

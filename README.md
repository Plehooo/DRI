# DRI — Direct Stream Player

A native Android player for authorized HTTPS HLS (`.m3u8`) and MPEG-DASH (`.mpd`) manifests.

## Features
- Native AndroidX Media3 player; no YouTube web embed.
- HLS and DASH playback with adaptive formats handled by Media3.
- Paste a manifest URL from the clipboard.
- Remembers the last manifest URL locally.
- GitHub Actions builds a debug APK on every push to `main`.

## Important limitation
A YouTube or YouTube Kids watch-page URL is not itself an HLS/DASH manifest. DRI does not extract private/internal Google stream URLs and does not bypass sign-in, signed-token rules, DRM, or service restrictions. Use a manifest URL that the stream owner/provider has made available for direct playback and that you are authorized to play.

## Build
Open the latest **Build DRI APK** run under the repository's Actions tab and download the `DRI-debug-APK` artifact.

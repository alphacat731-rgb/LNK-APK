# LNK-APK

One-tap website → fullscreen Android APK.

## How it works

Phone → GitHub Pages → Cloudflare Worker → GitHub Actions → APK Release.

The Pages site collects the app name, URL and PNG/JPG icon. The Worker keeps your GitHub token secret and dispatches the Android build. The APK is uploaded to a GitHub Release and the page gives you the download link.

## Setup

1. Keep this repository on `main`.
2. Enable **Settings → Pages → GitHub Actions**.
3. Deploy `worker/src/index.js` to Cloudflare Workers.
4. In the Worker, add secrets:
   - `GITHUB_TOKEN`: a fine-grained token for this repo with Actions write + Contents write.
   - `BUILD_PASSWORD`: your private builder password.
5. Set Worker variables `GITHUB_OWNER=alphacat731-rgb`, `GITHUB_REPO=LNK-APK`, `GITHUB_WORKFLOW=build-apk.yml`.
6. Put the Worker URL in `site/config.js`.
7. Open the GitHub Pages URL on your phone.

## Result

The generated app is a native Android WebView with no browser/address/search bar. Android back navigates the site.

Each build uses a temporary signing key, so separate builds are separate APK identities. For normal upgrade-in-place behavior, replace the temporary key step with a persistent keystore stored as GitHub Actions secrets.

Only wrap websites you own or are authorized to package.

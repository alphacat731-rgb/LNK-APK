# LNK-APK

Simple website → fullscreen Android APK builder that works using **GitHub only**.

No Cloudflare Worker, no API token, and no external backend.

## Build an APK from your phone

1. Open the **Actions** tab in this repository.
2. Select **Build APK**.
3. Tap **Run workflow**.
4. Enter:
   - **App name** — the name shown by Android.
   - **Website URL** — for example `https://example.com`.
   - **Icon path** — optional. Upload a PNG/JPG into the repo first, for example `assets/icon.png`, then enter that exact path.
5. Tap **Run workflow**.
6. Wait for the workflow to finish.
7. Open the completed workflow and download **LNK-APK** from the **Artifacts** section.

GitHub supports manually running workflows that use the `workflow_dispatch` trigger from the Actions tab.


## GrokBot Avatar Hub download page

The public download site is:

**https://alphacat731-rgb.github.io/LNK-APK/**

It is a static GitHub Pages site with a direct **Download APK** button. New builds are published to GitHub Releases automatically by the workflow.

### Installing the APK

1. Open the download page.
2. Tap **Download APK**.
3. Open the downloaded `.apk` file.
4. If Android asks, allow your browser/file manager to install apps from that source.
5. Install **GrokBot Avatar Hub**.

Google Play is not required.

## App behavior

The generated APK is a native Android WebView:

- No browser/address/search bar.
- Fullscreen immersive mode.
- Android back button navigates back through the website.
- JavaScript and DOM storage are enabled for modern sites.
- HTTP and HTTPS websites are supported.

## Icon

PNG, JPG, JPEG, and WebP icons are supported. Keep the uploaded icon reasonably small (under about 60 KB).

If no custom icon is supplied, the project uses its built-in fallback icon.

## Signing

Each build currently uses a temporary signing key. That means separate builds are separate Android signing identities and are not intended as seamless upgrades of one another.

For personal testing and one-off APKs, this is fine. A persistent keystore can be added later if you want update-in-place installs.

## Important

Only package websites you own or are authorized to package.

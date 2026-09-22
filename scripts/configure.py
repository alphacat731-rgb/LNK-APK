import json
import os
import re
from pathlib import Path
from xml.sax.saxutils import escape

R = Path(__file__).resolve().parents[1]
name = os.getenv("APP_NAME", "GrokBot Avatar Hub").strip() or "GrokBot Avatar Hub"
sites_raw = os.getenv("SITES", "").strip()
icon_path = os.getenv("ICON_PATH", "").strip() or "assets/grok_icon.webp"
background = os.getenv("BACKGROUND_COLOR", "#111111").strip()
card = os.getenv("CARD_COLOR", "#202020").strip()
text_color = os.getenv("TEXT_COLOR", "#FFFFFF").strip()
version_name = os.getenv("VERSION_NAME", "1.0").strip() or "1.0"
version_code = int(os.getenv("VERSION_CODE", "1"))

if version_code < 1:
    raise SystemExit("VERSION_CODE must be 1 or higher")

if not sites_raw:
    raise SystemExit("SITES must contain at least one editor")

def valid_color(value):
    return bool(re.fullmatch(r"#[0-9A-Fa-f]{6}", value))

for label, value in [("background_color", background), ("card_color", card), ("text_color", text_color)]:
    if not valid_color(value):
        raise SystemExit(f"{label} must be a 6-digit hex color such as #111111")

apps = []
for raw in sites_raw.replace("\r\n", "\n").replace("\r", "\n").splitlines():
    raw = raw.strip()
    if not raw:
        continue
    if "|" not in raw:
        raise SystemExit(f"Invalid site entry: {raw}. Use Name | https://example.com")
    label, url = [part.strip() for part in raw.split("|", 1)]
    if not label or not url:
        raise SystemExit(f"Invalid site entry: {raw}")
    if not re.match(r"^https?://", url):
        raise SystemExit(f"URL must start with http:// or https://: {url}")
    apps.append({"name": label, "url": url})

if not apps:
    raise SystemExit("No valid editor entries found")

assets = R / "app/src/main/assets"
assets.mkdir(parents=True, exist_ok=True)
(assets / "apps.json").write_text(json.dumps({
    "background": background,
    "card": card,
    "text": text_color,
    "apps": apps
}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

escaped_version_name = version_name.replace("\\", "\\\\").replace('"', '\\"')
gradle = f'''plugins {{ id("com.android.application") }}
android {{ namespace="com.websitetodapk.app"; compileSdk=36
    defaultConfig {{ applicationId="com.websitetodapk.app"; minSdk=23; targetSdk=36; versionCode={version_code}; versionName="{escaped_version_name}" }}
    buildFeatures {{ buildConfig=true }}
    buildTypes {{ release {{ isMinifyEnabled = false }} }}
}}
'''
(R / "app/build.gradle.kts").write_text(gradle, encoding="utf-8")
(R / "app/src/main/res/values/strings.xml").write_text(
    "<resources><string name=\"app_name\">" + escape(name) + "</string></resources>\n",
    encoding="utf-8"
)

if icon_path:
    p = (R / icon_path).resolve()
    if not str(p).startswith(str(R.resolve()) + os.sep) or not p.is_file():
        raise SystemExit(f"Icon file not found inside repository: {icon_path}")
    if p.suffix.lower() not in {".png", ".jpg", ".jpeg", ".webp"}:
        raise SystemExit("Icon must be PNG, JPG, JPEG, or WebP")
    raw = p.read_bytes()
    if len(raw) > 60000:
        raise SystemExit("Icon file is too large; use an image under 60 KB")
    ext = "webp" if p.suffix.lower() == ".webp" else ("png" if p.suffix.lower() == ".png" else "jpg")
    drawable_dir = R / "app/src/main/res/drawable-nodpi"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    out = drawable_dir / f"grokbot_launcher_icon.{ext}"
    out.write_bytes(raw)

    # Keep the launcher icon uniquely named and density-independent.
    # Remove legacy icon resources so stale fallbacks cannot be packaged.
    for directory in [
        R / "app/src/main/res/drawable",
        R / "app/src/main/res/drawable-nodpi",
    ]:
        for old in [
            "app_icon.xml", "app_icon.webp", "app_icon.png",
            "app_icon.jpg", "app_icon.jpeg",
            "grokbot_launcher_icon.webp", "grokbot_launcher_icon.png",
            "grokbot_launcher_icon.jpg", "grokbot_launcher_icon.jpeg",
        ]:
            q = directory / old
            if q.exists() and q != out:
                q.unlink()

import os, re
from pathlib import Path
from xml.sax.saxutils import escape

R = Path(__file__).resolve().parents[1]
name = os.getenv("APP_NAME", "My Web App").strip() or "My Web App"
url = os.getenv("WEBSITE_URL", "https://example.com").strip()
icon_path = os.getenv("ICON_PATH", "").strip()

if not re.match(r"^https?://", url):
    raise SystemExit("website_url must start with http:// or https://")

u = url.replace("\\", "\\\\").replace('"', '\\"')
gradle = f'''plugins {{ id("com.android.application") }}
android {{ namespace="com.websitetodapk.app"; compileSdk=35
    defaultConfig {{ applicationId="com.websitetodapk.app"; minSdk=23; targetSdk=35; versionCode=1; versionName="1.0"; buildConfigField("String","WEBSITE_URL","\\\"{u}\\\""); buildConfigField("boolean","FULLSCREEN","true") }}
    buildFeatures {{ buildConfig=true }}
    buildTypes {{ release {{ isMinifyEnabled = false }} }}
}}
'''
(R / "app/build.gradle.kts").write_text(gradle)
(R / "app/src/main/res/values/strings.xml").write_text(
    "<resources><string name=\"app_name\">" + escape(name) + "</string></resources>\n"
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
    out = R / f"app/src/main/res/drawable/app_icon.{ext}"
    out.write_bytes(raw)
    for old in ["app_icon.xml", "app_icon.webp", "app_icon.png", "app_icon.jpg", "app_icon.jpeg"]:
        q = R / "app/src/main/res/drawable" / old
        if q.exists() and q != out:
            q.unlink()

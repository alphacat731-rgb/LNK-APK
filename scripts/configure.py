import base64,os,re
from pathlib import Path
from xml.sax.saxutils import escape
R=Path(__file__).resolve().parents[1]
name=os.getenv("APP_NAME","My Web App").strip() or "My Web App"
url=os.getenv("WEBSITE_URL","https://example.com").strip()
b=os.getenv("ICON_BASE64","").strip(); mime=os.getenv("ICON_MIME","image/webp").lower()
if not re.match(r"^https?://",url): raise SystemExit("website_url must start with http:// or https://")
u=url.replace("\\","\\\\").replace('"','\\\"')
gradle=f'''plugins {{ id("com.android.application") }}
android {{ namespace="com.websitetodapk.app"; compileSdk=35
    defaultConfig {{ applicationId="com.websitetodapk.app"; minSdk=23; targetSdk=35; versionCode=1; versionName="1.0"; buildConfigField("String","WEBSITE_URL","\\\"{u}\\\""); buildConfigField("boolean","FULLSCREEN","true") }}
    buildFeatures {{ buildConfig=true }}
    buildTypes {{ release {{ minifyEnabled=false }} }}
}}
'''
(R/"app/build.gradle.kts").write_text(gradle)
(R/"app/src/main/res/values/strings.xml").write_text("<resources><string name=\"app_name\">"+escape(name)+"</string></resources>\n")
if b:
    raw=base64.b64decode(b,validate=True)
    if len(raw)>60000: raise SystemExit("Icon payload too large")
    ext="webp" if mime=="image/webp" or raw[:4]==b"RIFF" else ("png" if mime=="image/png" else "jpg")
    (R/f"app/src/main/res/drawable/app_icon.{ext}").write_bytes(raw)
    q=R/"app/src/main/res/drawable/app_icon.xml"
    if q.exists(): q.unlink()

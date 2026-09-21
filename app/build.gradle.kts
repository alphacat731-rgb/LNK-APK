plugins { id("com.android.application") }

android {
    namespace = "com.websitetodapk.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.websitetodapk.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "WEBSITE_URL", "\"https://example.com\"")
        buildConfigField("boolean", "FULLSCREEN", "true")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

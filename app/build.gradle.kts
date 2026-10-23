plugins {
    alias(libs.plugins.nfcpay.android.application)
    alias(libs.plugins.nfcpay.android.application.compose)
}

android {
    namespace = "com.danyastry.nfcpay"

    defaultConfig {
        applicationId = "com.danyastry.nfcpay"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.core.nfc)
    implementation(projects.core.ui)

    implementation(projects.feature.terminal)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
}

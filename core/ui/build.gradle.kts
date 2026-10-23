plugins {
    alias(libs.plugins.nfcpay.android.library.compose)
}

android {
    namespace = "com.danyastry.nfcpay.core.ui"
}

dependencies {
    api(libs.androidx.material3)
}

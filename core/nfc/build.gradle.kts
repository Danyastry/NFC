plugins {
    alias(libs.plugins.nfcpay.android.library.compose)
}

android {
    namespace = "com.danyastry.nfcpay.core.nfc"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
}

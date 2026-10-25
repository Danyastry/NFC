plugins {
    alias(libs.plugins.nfcpay.android.library)
}

android {
    namespace = "com.danyastry.nfcpay.core.ndef"
}

dependencies {
    implementation(projects.core.emv)
}

plugins {
    alias(libs.plugins.nfcpay.android.feature)
}

android {
    namespace = "com.danyastry.nfcpay.feature.tags"
}

dependencies {
    implementation(projects.core.ndef)
}

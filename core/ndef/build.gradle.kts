plugins {
    alias(libs.plugins.nfcpay.android.library)
}

android {
    namespace = "com.danyastry.nfcpay.core.ndef"
}

dependencies {
    api(projects.core.emv)

    testImplementation(libs.kotlinx.coroutines.test)
}

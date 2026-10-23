plugins {
    alias(libs.plugins.nfcpay.android.library)
}

android {
    namespace = "com.danyastry.nfcpay.core.emv"
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}

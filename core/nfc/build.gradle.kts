plugins {
    alias(libs.plugins.nfcpay.android.library.compose)
}

android {
    namespace = "com.danyastry.nfcpay.core.nfc"
}

dependencies {
    api(projects.core.emv)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

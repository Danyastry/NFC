package com.danyastry.nfcpay

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danyastry.nfcpay.catalog.FeatureCatalog
import com.danyastry.nfcpay.catalog.FeatureRegistry
import com.danyastry.nfcpay.core.ui.theme.NfcPayTheme
import com.danyastry.nfcpay.navigation.NfcPayNavHost

class MainActivity : ComponentActivity() {

    private val catalog = FeatureCatalog(FeatureRegistry.features, deviceSdk = Build.VERSION.SDK_INT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NfcPayTheme {
                NfcPayNavHost(catalog)
            }
        }
    }
}

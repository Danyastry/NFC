package com.danyastry.nfcpay.feature.issuer.logic

import com.danyastry.nfcpay.core.emv.card.DemoCards
import com.danyastry.nfcpay.core.emv.issuer.DemoIssuer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IssuerViewsTest {

    @Test
    fun `formats money in minor units`() {
        assertThat(IssuerViews.money(250_00)).isEqualTo("250.00")
        assertThat(IssuerViews.money(5)).isEqualTo("0.05")
    }

    @Test
    fun `shows a fresh account`() {
        val view = IssuerViews.account(DemoIssuer.account())

        assertThat(view.pan).isEqualTo("9999 0012 3456 7891")
        assertThat(view.balance).isEqualTo("250.00")
        assertThat(view.lastAtc).isEqualTo("none")
    }

    @Test
    fun `shows only check values of the keys`() {
        val keys = IssuerViews.keys(DemoCards.issuerMasterKey, DemoIssuer.account())

        assertThat(keys.issuerMasterKeyCheck).isEqualTo("08D7B4")
        assertThat(keys.cardKeyCheck).isEqualTo("06DC71")
    }
}

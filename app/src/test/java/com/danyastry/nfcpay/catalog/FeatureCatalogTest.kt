package com.danyastry.nfcpay.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danyastry.nfcpay.core.ui.FeatureCategory
import com.danyastry.nfcpay.core.ui.FeatureEntry
import com.danyastry.nfcpay.core.ui.FeatureInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FeatureCatalogTest {

    @Test
    fun `empty catalog has no sections`() {
        val catalog = FeatureCatalog(emptyList(), deviceSdk = 35)

        assertThat(catalog.isEmpty).isTrue()
        assertThat(catalog.sections).isEmpty()
    }

    @Test
    fun `groups features into sections ordered by category`() {
        val card = feature("card-emulation", "Card emulation", FeatureCategory.PAYMENTS)
        val arqc = feature("arqc", "ARQC", FeatureCategory.CRYPTOGRAMS)
        val relay = feature("relay-attack", "Relay attack", FeatureCategory.ATTACKS)

        val catalog = FeatureCatalog(listOf(relay, arqc, card), deviceSdk = 35)

        assertThat(catalog.sections.map { it.category }).containsExactly(
            FeatureCategory.PAYMENTS,
            FeatureCategory.CRYPTOGRAMS,
            FeatureCategory.ATTACKS,
        ).inOrder()
    }

    @Test
    fun `sorts features inside a section by name`() {
        val terminal = feature("terminal", "Terminal", FeatureCategory.PAYMENTS)
        val card = feature("card-emulation", "Card emulation", FeatureCategory.PAYMENTS)

        val catalog = FeatureCatalog(listOf(terminal, card), deviceSdk = 35)

        assertThat(catalog.sections.single().entries).containsExactly(card, terminal).inOrder()
    }

    @Test
    fun `features newer than the device are listed but not supported`() {
        val future = feature("future", "Future feature", FeatureCategory.TAGS, minSdk = 31)

        val catalog = FeatureCatalog(listOf(future), deviceSdk = 30)

        assertThat(catalog.sections.single().entries).containsExactly(future)
        assertThat(catalog.isSupported(future)).isFalse()
        assertThat(catalog.find("future")).isNull()
    }

    @Test
    fun `finds supported features by id`() {
        val future = feature("future", "Future feature", FeatureCategory.TAGS, minSdk = 31)

        val catalog = FeatureCatalog(listOf(future), deviceSdk = 31)

        assertThat(catalog.isSupported(future)).isTrue()
        assertThat(catalog.find("future")).isSameInstanceAs(future)
        assertThat(catalog.find("unknown")).isNull()
    }

    @Test
    fun `rejects duplicate ids`() {
        val first = feature("terminal", "Terminal", FeatureCategory.PAYMENTS)
        val second = feature("terminal", "SoftPOS", FeatureCategory.PAYMENTS)

        val error = assertThrows(IllegalArgumentException::class.java) { FeatureCatalog(listOf(first, second), deviceSdk = 35) }
        assertThat(error).hasMessageThat().contains("terminal")
    }

    @Test
    fun `registry has unique ids`() {
        FeatureCatalog(FeatureRegistry.features, deviceSdk = 35)
    }

    private fun feature(id: String, name: String, category: FeatureCategory, minSdk: Int = 26): FeatureEntry =
        object : FeatureEntry {
            override val info = FeatureInfo(id = id, name = name, category = category, summary = "$name summary", minSdk = minSdk)

            @Composable
            override fun Content(modifier: Modifier) = Unit
        }
}

package com.danyastry.nfcpay.core.nfc.radio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NfcAvailabilityTest {

    @Test
    fun `no hardware wins over every other flag`() {
        val availability = NfcAvailability.resolve(NfcState(hasNfc = false, hasCardEmulation = true, isEnabled = true))

        assertThat(availability).isEqualTo(NfcAvailability.NoNfc)
        assertThat(availability.canUseRadio).isFalse()
    }

    @Test
    fun `hardware that is switched off is off`() {
        val availability = NfcAvailability.resolve(NfcState(hasNfc = true, hasCardEmulation = true, isEnabled = false))

        assertThat(availability).isEqualTo(NfcAvailability.Off)
        assertThat(availability.canUseRadio).isFalse()
    }

    @Test
    fun `enabled hardware is ready and reports card emulation`() {
        assertThat(NfcAvailability.resolve(NfcState(hasNfc = true, hasCardEmulation = true, isEnabled = true)))
            .isEqualTo(NfcAvailability.Ready(canEmulateCards = true))
        assertThat(NfcAvailability.resolve(NfcState(hasNfc = true, hasCardEmulation = false, isEnabled = true)))
            .isEqualTo(NfcAvailability.Ready(canEmulateCards = false))
    }
}

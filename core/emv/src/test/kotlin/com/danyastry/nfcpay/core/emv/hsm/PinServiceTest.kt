package com.danyastry.nfcpay.core.emv.hsm

import com.danyastry.nfcpay.core.emv.dukpt.AesDukpt
import com.danyastry.nfcpay.core.emv.dukpt.DukptTerminal
import com.danyastry.nfcpay.core.emv.dukpt.EncryptedPin
import com.danyastry.nfcpay.core.emv.dukpt.KeySerialNumber
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinServiceTest {

    private val bdk = "FEDCBA9876543210F1F1F1F1F1F1F1F1".hexToBytes()
    private val ikid = "1234567890123456".hexToBytes()
    private val pan = "9999001234567891"
    private val terminal = DukptTerminal(AesDukpt.initialKey(bdk, ikid), ikid)
    private val service = PinService(PinHsm(bdk, "0123456789ABCDEFFEDCBA9876543210".hexToBytes())).apply { enroll(pan, "2468") }

    @Test
    fun `stores a PVV, never the PIN`() {
        val record = service.record(pan)!!

        assertThat(record.pvv).hasLength(4)
        assertThat(record.pvv).isNotEqualTo("2468")
        assertThat(record.triesLeft).isEqualTo(3)
    }

    @Test
    fun `verifies a PIN typed on a DUKPT terminal`() {
        val verdict = service.verify(pan, terminal.encryptPin("2468", pan))

        assertThat(verdict.check).isEqualTo(PinCheck.VERIFIED)
        assertThat(verdict.triesLeft).isEqualTo(3)
    }

    @Test
    fun `blocks the card after three wrong PINs`() {
        assertThat(service.verify(pan, terminal.encryptPin("1111", pan)).triesLeft).isEqualTo(2)
        assertThat(service.verify(pan, terminal.encryptPin("1111", pan)).triesLeft).isEqualTo(1)
        val third = service.verify(pan, terminal.encryptPin("1111", pan))

        assertThat(third.check).isEqualTo(PinCheck.WRONG_PIN)
        assertThat(third.blocked).isTrue()
        val afterBlock = service.verify(pan, terminal.encryptPin("2468", pan))
        assertThat(afterBlock.blocked).isTrue()
        assertThat(afterBlock.check).isNull()

        service.unblock(pan)
        assertThat(service.verify(pan, terminal.encryptPin("2468", pan)).isVerified).isTrue()
    }

    @Test
    fun `a correct PIN resets the tries`() {
        service.verify(pan, terminal.encryptPin("1111", pan))

        assertThat(service.verify(pan, terminal.encryptPin("2468", pan)).triesLeft).isEqualTo(3)
    }

    @Test
    fun `a broken block or serial number does not cost a try`() {
        val good = terminal.encryptPin("2468", pan)
        val tampered = EncryptedPin(good.ksn, good.block.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() })
        val wrongKsn = EncryptedPin(KeySerialNumber(ikid, 0x1FFFFL), good.block)

        assertThat(service.verify(pan, tampered).check).isEqualTo(PinCheck.BAD_BLOCK)
        assertThat(service.verify(pan, wrongKsn).check).isEqualTo(PinCheck.BAD_KSN)
        assertThat(service.record(pan)!!.triesLeft).isEqualTo(3)
    }

    @Test
    fun `a PIN bound to another card fails`() {
        val forOtherCard = terminal.encryptPin("2468", "4111111111111111")

        assertThat(service.verify(pan, forOtherCard).check).isEqualTo(PinCheck.BAD_BLOCK)
    }

    @Test
    fun `unknown cards are treated as blocked`() {
        assertThat(service.verify("4111111111111111", terminal.encryptPin("2468", "4111111111111111")).blocked).isTrue()
    }
}

package com.danyastry.nfcpay.core.emv.dukpt

import com.danyastry.nfcpay.core.emv.pin.IsoPinBlock
import com.danyastry.nfcpay.core.emv.util.hexToBytes
import com.danyastry.nfcpay.core.emv.util.toHex
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AesDukptTest {

    private val bdk = "FEDCBA9876543210F1F1F1F1F1F1F1F1".hexToBytes()
    private val initialKeyId = "1234567890123456".hexToBytes()
    private val initialKey = AesDukpt.initialKey(bdk, initialKeyId)

    private val transactionKeys = listOf(
        "4F21B565BAD9835E112B6465635EAE44", "2F34D68DE10F68D38091A73B9E7C437C",
        "031504E530365CF812642385405183 18".replace(" ", ""), "0EEFC7ADA628BA68878DA9165A8A1887",
        "C2A7AC328A5DA2D6002D62465BFC028B", "D30F7D9351DA58448A2F5E92B4EE3B7D",
        "A8253CEED9AC042C54F75D35C8352278", "718EE6CF0B27E53D5F7AF99C4D8146A2",
    )
    private val pinKeys = listOf(
        "AF8CB133A78F8DC2D1359F18527593FB", "D30BDC73EC9714B000BEC66BDB7B6D09",
        "7D69F01F3B45449F62C7816ECE723268", "91A0588318EC2673214271F70137896E",
        "35A43BC9EFEB09C756204B57E3FB7D4D", "02DCC6CD1201A3A2CA7099559C862123",
        "6ECF912F3B18CA11A7A27BB60705FD09", "4D9DF3FBEE3448FC3E676D04320A90F5",
    )
    private val macKeys = listOf(
        "A2DC23DE6FDE0824A2BC321E08E4B8B7", "484C3B06E8562704528CD5B46FB12FB6",
        "A5DF7D9D800CA769766F0C77CA4E6E6C", "E6E4679647D8A9057C1FC15537CB4C4E",
        "0588185FE1FF8C7E22FAD78C1C61F065", "7B60F72A5639C0EE7EB2100FA80BB793",
        "BAA08CA263C69525BC6B1BA8F4275D69", "6FD572E5D59E618875F193484F9178FB",
    )
    private val dataKeys = listOf(
        "A35C412EFD41FDB98B69797C02DCD08F", "D639514AA33AC43AD9229E433D6D4E5B",
        "EF17F6AB45B4820C93A3DCB21BC491AD", "B3BD44C08BB6BA27C3BB4711D7D70387",
        "CA02DF6F30B39E14BD0B4A30E460920F", "C9B8A7C4E486180B2229115164F0B293",
        "0FA8F1F0A2DD7B1005A862D77CDED698", "650F34204ABD4E57764D61AC3D266FB1",
    )
    private val pinBlocks = listOf(
        "A912150391AB65A67E52883D81CE2D15", "52A00503BD34BA1383F6A7EE9FE2547F",
        "A5A27E82B43A9A866A93D7ABE89CEF93", "71B3D0528669498777555A8BE6698E44",
        "881A7F77A2E04E5BEA985E342FD0B628", "BDC1C3871AFB0B340AA5B5CEFD08695E",
        "4A8E6B8C7DBEE6CBA6DC774F0CB83396", "8308BB857C17F390369F761F8EB358FA",
    )

    @Test
    fun `derives the initial key of the X9 24-3 supplement`() {
        assertThat(initialKey.toHex()).isEqualTo("1273671EA26AC29AFA4D1084127652A1")
    }

    @Test
    fun `builds derivation data as table 3 of the standard`() {
        assertThat(AesDukpt.derivationData(DukptKeyUsage.INITIAL_KEY, initialKeyId).toHex()).isEqualTo("0101800100020080" + "1234567890123456")
        assertThat(AesDukpt.derivationData(DukptKeyUsage.PIN_ENCRYPTION, "9012345600000001".hexToBytes()).toHex())
            .isEqualTo("0101100000020080" + "9012345600000001")
    }

    @Test
    fun `derives the first eight transaction and working keys`() {
        (1..8).forEach { counter ->
            val ksn = KeySerialNumber(initialKeyId, counter.toLong())
            val transactionKey = AesDukpt.transactionKey(initialKey, ksn)

            assertThat(transactionKey.toHex()).isEqualTo(transactionKeys[counter - 1])
            assertThat(AesDukpt.workingKey(transactionKey, DukptKeyUsage.PIN_ENCRYPTION, ksn).toHex()).isEqualTo(pinKeys[counter - 1])
            assertThat(AesDukpt.workingKey(transactionKey, DukptKeyUsage.MAC_GENERATION, ksn).toHex()).isEqualTo(macKeys[counter - 1])
            assertThat(AesDukpt.workingKey(transactionKey, DukptKeyUsage.DATA_ENCRYPTION, ksn).toHex()).isEqualTo(dataKeys[counter - 1])
        }
    }

    @Test
    fun `encrypts the supplement PIN blocks`() {
        val pinField = "441234AAAAAAAAAA2F69ADDE2E9E7ACE".hexToBytes()

        (1..8).forEach { counter ->
            val ksn = KeySerialNumber(initialKeyId, counter.toLong())
            val pinKey = AesDukpt.hostWorkingKey(bdk, ksn, DukptKeyUsage.PIN_ENCRYPTION)
            val block = IsoPinBlock.encipherFormat4Field(pinKey, pinField, "4111111111111111")

            assertThat(block.toHex()).isEqualTo(pinBlocks[counter - 1])
            assertThat(IsoPinBlock.decipherFormat4(pinKey, block, "4111111111111111")).isEqualTo("1234")
        }
    }

    @Test
    fun `derives keys at the counter boundaries`() {
        val expected = mapOf(
            0x1FFFEL to "E21E8C8D347F8561A2BE752DAA85A111",
            0x20000L to "F7AE9025468A25D37B7249CFFED224C8",
            0x20001L to "692C8EA4013645135B36497131DF9D2C",
            0xFFFE2000L to "48E585B694EB0B18D5C35443E163C0BA",
            0xFFFE4000L to "396C2C7CA1EA701C03B86B7D41F0C562",
            0xFFFE8000L to "0387625F189B58AE03EF0E8CCA41105E",
            0xFFFF0000L to "F6BA59389BD14A9855BE9727E7C52E3C",
        )

        expected.forEach { (counter, key) ->
            assertThat(AesDukpt.transactionKey(initialKey, KeySerialNumber(initialKeyId, counter)).toHex()).isEqualTo(key)
        }
    }

    @Test
    fun `refuses counters with more than sixteen bits set`() {
        val tooMany = KeySerialNumber(initialKeyId, 0x1FFFFL)

        assertThat(tooMany.isUsable).isFalse()
        assertThrows(IllegalArgumentException::class.java) { AesDukpt.transactionKey(initialKey, tooMany) }
        assertThrows(IllegalArgumentException::class.java) { AesDukpt.transactionKey(initialKey, KeySerialNumber(initialKeyId, 0)) }
    }

    @Test
    fun `reads and writes twelve byte serial numbers`() {
        val ksn = KeySerialNumber.parse("123456789012345600000001")

        assertThat(ksn.counter).isEqualTo(1)
        assertThat(ksn.derivationId.toHex()).isEqualTo("90123456")
        assertThat(KeySerialNumber.parse("1234567890123456FFFF0000").counter).isEqualTo(0xFFFF0000L)
        assertThat(ksn.encode().toHex()).isEqualTo("123456789012345600000001")
    }
}

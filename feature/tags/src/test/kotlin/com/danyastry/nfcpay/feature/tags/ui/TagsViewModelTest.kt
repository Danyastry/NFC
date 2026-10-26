package com.danyastry.nfcpay.feature.tags.ui

import com.danyastry.nfcpay.core.ndef.message.NdefMessage
import com.danyastry.nfcpay.core.ndef.record.TextRecord
import com.danyastry.nfcpay.core.ndef.type4.Type4Tag
import com.danyastry.nfcpay.feature.tags.model.RecordKind
import com.danyastry.nfcpay.feature.tags.model.TagAction
import com.danyastry.nfcpay.feature.tags.model.TagSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {

    private val tag = Type4Tag()
    private lateinit var viewModel: TagsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TagsViewModel(tag)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `writes a link and reads it back`() = runTest {
        viewModel.onKindChange(RecordKind.LINK)
        viewModel.onInputChange("example.com")
        viewModel.onWrite()
        advanceUntilIdle()
        assertThat(viewModel.state.value.result!!.action).isEqualTo(TagAction.WRITE)
        assertThat(viewModel.state.value.result!!.error).isNull()

        viewModel.onRead()
        advanceUntilIdle()

        val result = viewModel.state.value.result!!
        assertThat(result.records.single().content).isEqualTo("https://example.com")
        assertThat(result.exchanges.first().label).isEqualTo("SELECT NDEF application")
    }

    @Test
    fun `reading an empty virtual tag explains why`() = runTest {
        viewModel.onRead()
        advanceUntilIdle()

        assertThat(viewModel.state.value.result!!.error).isEqualTo("The tag holds no message")
    }

    @Test
    fun `cannot write blank text or to a real tag`() = runTest {
        viewModel.onWrite()
        assertThat(viewModel.state.value.result).isNull()

        viewModel.onInputChange("hello")
        viewModel.onSourceChange(TagSource.NFC)
        assertThat(viewModel.state.value.canWrite).isFalse()
    }

    @Test
    fun `decodes a real tag and reports one without NDEF`() = runTest {
        viewModel.onSourceChange(TagSource.NFC)
        viewModel.onRead()
        assertThat(viewModel.state.value.waitingForTag).isTrue()

        viewModel.onRealTag { NdefMessage(TextRecord("from a sticker").toRecord()).encode() }
        advanceUntilIdle()
        assertThat(viewModel.state.value.result!!.records.single().content).isEqualTo("from a sticker")

        viewModel.onRead()
        viewModel.onRealTag { null }
        advanceUntilIdle()
        assertThat(viewModel.state.value.result!!.error).isEqualTo("The tag holds no NDEF message")

        viewModel.onRead()
        viewModel.onRealTag { throw IOException("gone") }
        advanceUntilIdle()
        assertThat(viewModel.state.value.result!!.error).isEqualTo("The tag left the field")
    }

    @Test
    fun `ignores tags that arrive while not waiting`() = runTest {
        var called = false
        viewModel.onRealTag { called = true; null }

        assertThat(called).isFalse()
    }
}

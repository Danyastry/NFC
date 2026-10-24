package com.danyastry.nfcpay.core.emv.issuer

import com.danyastry.nfcpay.core.emv.terminal.OnlineAuthorizer
import com.danyastry.nfcpay.core.emv.terminal.OnlineRequest
import com.danyastry.nfcpay.core.emv.terminal.OnlineResponse
import com.danyastry.nfcpay.core.emv.util.hexToBytes

class IssuerAuthorizer(
    private val host: IssuerHost,
    private val onResponse: (AuthorizationResponse) -> Unit = {},
) : OnlineAuthorizer {

    override suspend fun authorize(request: OnlineRequest): OnlineResponse {
        val response = host.authorize(
            AuthorizationRequest(
                pan = request.card.pan,
                panSequenceNumber = request.card.panSequenceNumber ?: 0,
                field55 = request.field55,
                date = request.date,
            ),
        )
        onResponse(response)
        return OnlineResponse(response.responseCode.code, response.issuerAuthenticationData?.hexToBytes())
    }
}

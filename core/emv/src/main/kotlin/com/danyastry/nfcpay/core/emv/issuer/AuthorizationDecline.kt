package com.danyastry.nfcpay.core.emv.issuer

class AuthorizationDecline(val reason: DeclineReason, val detail: String) : Exception(detail)

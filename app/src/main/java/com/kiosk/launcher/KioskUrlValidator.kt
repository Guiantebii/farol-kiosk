package com.kiosk.launcher

import android.net.Uri

object KioskUrlValidator {

    /**
     * Normaliza e valida URLs http/https para uso no WebView.
     * Adiciona https:// quando o esquema estiver ausente.
     */
    fun normalize(input: String): String? {
        var candidate = input.trim()
        if (candidate.isEmpty()) return null
        if (!candidate.contains("://")) {
            candidate = "https://$candidate"
        }
        val uri = Uri.parse(candidate)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return candidate
    }
}

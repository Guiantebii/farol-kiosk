package com.kiosk.launcher

import android.net.Uri

object KioskUrlValidator {

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

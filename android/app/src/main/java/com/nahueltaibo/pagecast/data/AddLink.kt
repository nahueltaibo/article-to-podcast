package com.nahueltaibo.pagecast.data

import java.net.URI
import java.net.URLDecoder

/** Reads the feed URL out of a `pagecast://add?feed=<url>` link, or null if the link isn't one. */
fun feedFromAddLink(link: String): String? {
    val uri = runCatching { URI(link) }.getOrNull() ?: return null
    if (uri.scheme != "pagecast" || uri.host != "add") return null
    val feed = uri.rawQuery.orEmpty().split("&")
        .map { it.substringBefore("=") to it.substringAfter("=", "") }
        .firstOrNull { it.first == "feed" }?.second
        ?.let { URLDecoder.decode(it, "UTF-8") }
        ?.trim()
    return feed?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}

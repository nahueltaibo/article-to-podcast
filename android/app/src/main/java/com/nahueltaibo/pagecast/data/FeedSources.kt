package com.nahueltaibo.pagecast.data

/** The Pagecast feed every install starts with, stored like any feed the listener added. */
object FeedSources {
    /** Narrated Anthropic articles, served from the pagecast R2 bucket. */
    const val DEFAULT = "https://pagecast.taibo.ca/podcast.xml"

    /** Where earlier builds read the same feed. That GitHub Pages site is being retired. */
    const val LEGACY = "https://nahueltaibo.github.io/pagecast-podcasts/podcast.xml"

    /**
     * The listener's feeds after this version first runs. [DEFAULT] is added once, unless the
     * listener turned the old recommended-feed switch off. Removing it later sticks.
     */
    fun seed(feeds: Set<String>, alreadySeeded: Boolean, optedOut: Boolean): Set<String> {
        val migrated = feeds.map { if (it == LEGACY) DEFAULT else it }.toSet()
        return if (alreadySeeded || optedOut) migrated else migrated + DEFAULT
    }
}

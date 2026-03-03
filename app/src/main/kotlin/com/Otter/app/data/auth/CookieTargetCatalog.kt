package com.Otter.app.data.auth

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieTargetCatalog
    @Inject
    constructor(
        private val customCookieTargetStore: CustomCookieTargetStore,
    ) {
        companion object {
            const val TARGET_YOUTUBE = "youtube"
            const val TARGET_INSTAGRAM = "instagram"
            const val TARGET_TWITTER = "twitter"
            const val TARGET_REDDIT = "reddit"
        }

        val predefinedTargets: List<CookieTarget> =
            listOf(
                CookieTarget(
                    id = TARGET_YOUTUBE,
                    title = "YouTube",
                    loginUrl = "https://accounts.google.com/signin/v2/identifier?service=youtube",
                    domains = listOf("youtube.com", "youtu.be", "music.youtube.com", "google.com", "accounts.google.com"),
                ),
                CookieTarget(
                    id = TARGET_INSTAGRAM,
                    title = "Instagram",
                    loginUrl = "https://www.instagram.com/accounts/login/",
                    domains = listOf("instagram.com"),
                ),
                CookieTarget(
                    id = TARGET_TWITTER,
                    title = "Twitter/X",
                    loginUrl = "https://x.com/i/flow/login",
                    domains = listOf("x.com", "twitter.com"),
                ),
                CookieTarget(
                    id = TARGET_REDDIT,
                    title = "Reddit",
                    loginUrl = "https://www.reddit.com/login/",
                    domains = listOf("reddit.com"),
                ),
            )

        val customTargets: Flow<List<CookieTarget>> = customCookieTargetStore.targets

        val allTargets: Flow<List<CookieTarget>> =
            combine(
                flowOf(predefinedTargets),
                customTargets,
            ) { predefined, custom ->
                predefined + custom
            }

        suspend fun getAllTargetsOnce(): List<CookieTarget> = allTargets.first()

        suspend fun findById(id: String): CookieTarget? = getAllTargetsOnce().firstOrNull { it.id == id }

        suspend fun matchTargetIdForUrl(url: String): String? {
            val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
            if (host.isBlank()) return null

            val h = host.lowercase()
            return getAllTargetsOnce().firstOrNull { target ->
                target.domains.any { domain ->
                    val d = domain.lowercase()
                    h == d || h.endsWith(".$d")
                }
            }?.id
        }
    }

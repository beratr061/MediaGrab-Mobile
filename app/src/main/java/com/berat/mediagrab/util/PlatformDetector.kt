package com.berat.mediagrab.util

import androidx.compose.ui.graphics.Color
import com.berat.mediagrab.ui.theme.*

/** Supported video platforms */
enum class SupportedPlatform(
        val displayName: String,
        val color: Color,
        val iconAsset: String // Asset file name in assets/icons/
) {
    YOUTUBE("YouTube", YouTubeColor, "YouTube Logo.svg"),
    INSTAGRAM("Instagram", InstagramColor, "Instagram Logo.svg"),
    TIKTOK("TikTok", TikTokColor, "TikTok Logo.svg"),
    TWITTER("Twitter/X", TwitterColor, "X Logo.svg"),
    FACEBOOK("Facebook", FacebookColor, "Facebook Logo.svg"),
    TWITCH("Twitch", TwitchColor, "Twitch Logo.svg"),
    REDDIT("Reddit", RedditColor, "Reddit Logo.svg"),
    DISCORD("Discord", DiscordColor, "Discord Logo.svg"),
    SPOTIFY("Spotify", SpotifyColor, "Spotify Logo.svg"),
    PINTEREST("Pinterest", PinterestColor, "Pinterest Logo.svg"),
    UNKNOWN("Bağlantı", Primary, "")
}

/** Platform detector utility */
object PlatformDetector {

    private val platformPatterns =
            mapOf(
                    SupportedPlatform.YOUTUBE to
                            listOf("youtube.com", "youtu.be", "youtube-nocookie.com"),
                    SupportedPlatform.INSTAGRAM to listOf("instagram.com", "instagr.am"),
                    SupportedPlatform.TIKTOK to listOf("tiktok.com", "vm.tiktok.com"),
                    SupportedPlatform.TWITTER to listOf("twitter.com", "x.com", "t.co"),
                    SupportedPlatform.FACEBOOK to listOf("facebook.com", "fb.watch", "fb.com"),
                    SupportedPlatform.TWITCH to listOf("twitch.tv", "clips.twitch.tv"),
                    SupportedPlatform.REDDIT to listOf("reddit.com", "redd.it", "v.redd.it"),
                    SupportedPlatform.DISCORD to listOf("discord.com", "discord.gg"),
                    SupportedPlatform.SPOTIFY to listOf("spotify.com", "open.spotify.com"),
                    SupportedPlatform.PINTEREST to listOf("pinterest.com", "pin.it")
            )

    /** Detect platform from URL */
    fun detect(url: String): SupportedPlatform {
        if (url.isBlank()) return SupportedPlatform.UNKNOWN

        val lowerUrl = url.lowercase()

        for ((platform, patterns) in platformPatterns) {
            if (patterns.any { lowerUrl.contains(it) }) {
                return platform
            }
        }

        return SupportedPlatform.UNKNOWN
    }

    /** Get all supported platforms for display */
    fun getAllPlatforms(): List<SupportedPlatform> {
        return SupportedPlatform.entries.filter { it != SupportedPlatform.UNKNOWN }
    }
}

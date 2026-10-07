package com.example.data.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class AnonymousPersona(
    val name: String,
    val avatarEmoji: String,
    val badgeColorHex: String,
    val subtitle: String = "Anonymous Soul"
) {
    companion object {
        private val ADJECTIVES = listOf(
            "Midnight", "Velvet", "Whispering", "Silent", "Neon", "Cosmic",
            "Hidden", "Lunar", "Electric", "Mystic", "Starlight", "Shadow",
            "Dreaming", "Emerald", "Golden", "Echoing", "Amber", "Quiet"
        )

        private val NOUNS = listOf(
            "Soul", "Wanderer", "Voice", "Dreamer", "Ghost", "Poet",
            "Nomad", "Phantom", "Echo", "Seeker", "Specter", "Breeze",
            "Shadow", "Traveler", "Watcher", "Spirit", "Drifter", "Thinker"
        )

        private val EMOJIS = listOf(
            "🎭", "🌌", "🌙", "✨", "🔮", "🦊", "🦉", "🦋",
            "🌊", "🪶", "🕯️", "🪐", "🌿", "🌸", "🖤", "☁️"
        )

        private val BADGE_COLORS = listOf(
            "#9D7BFF", "#4EE2C0", "#FF6584", "#48CAE4",
            "#FFB84D", "#7C4DFF", "#00B0FF", "#00E676"
        )

        val PRESET_PERSONAS = listOf(
            AnonymousPersona("Midnight Soul", "🌙", "#9D7BFF", "Night Owl"),
            AnonymousPersona("Velvet Whisper", "🎭", "#FF6584", "Secret Keeper"),
            AnonymousPersona("Cosmic Nomad", "🌌", "#48CAE4", "Deep Thinker"),
            AnonymousPersona("Silent Dreamer", "✨", "#4EE2C0", "Quiet Heart"),
            AnonymousPersona("Mystic Echo", "🔮", "#7C4DFF", "Enigmatic Voice"),
            AnonymousPersona("Starlight Breeze", "🪶", "#00B0FF", "Gentle Wanderer"),
            AnonymousPersona("Electric Ghost", "⚡", "#FFB84D", "Curious Mind"),
            AnonymousPersona("Lunar Phantom", "🪐", "#A78BFA", "Wandering Star")
        )

        fun generateRandom(): AnonymousPersona {
            val num = Random.nextInt(1000, 9999)
            val adj = ADJECTIVES.random()
            val noun = NOUNS.random()
            val emoji = EMOJIS.random()
            val color = BADGE_COLORS.random()
            val name = if (Random.nextBoolean()) "$adj $noun" else "Anonymous #$num"
            return AnonymousPersona(name, emoji, color)
        }

        fun generateNumbered(seed: String = ""): AnonymousPersona {
            val num = if (seed.isNotEmpty()) {
                kotlin.math.abs(seed.hashCode() % 9000) + 1000
            } else {
                Random.nextInt(1000, 9999)
            }
            val emojiIndex = kotlin.math.abs(seed.hashCode()) % EMOJIS.size
            val colorIndex = kotlin.math.abs(seed.hashCode()) % BADGE_COLORS.size
            return AnonymousPersona("Anonymous #$num", EMOJIS[emojiIndex], BADGE_COLORS[colorIndex])
        }
    }
}

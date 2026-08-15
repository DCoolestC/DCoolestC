package com.isokovibe.musicplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the device's equalizer actually supports. Android does not promise a
 * fixed band count — most hardware exposes 5, some 10 — so the UI is built
 * from whatever the platform reports rather than assuming a shape.
 */
data class EqualizerCapabilities(
    val available: Boolean = false,
    val bandCount: Int = 0,
    /** Millibels. Typically around -1500..1500. */
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    /** Centre frequency per band, in Hz. */
    val centerFrequencies: List<Int> = emptyList(),
    val presetNames: List<String> = emptyList()
)

/** Settings applied to the audio pipeline. All levels are in millibels or
 *  0..1000 "strength" units, matching the platform effect APIs directly. */
data class AudioEffectSettings(
    val equalizerEnabled: Boolean = false,
    val bandLevels: List<Int> = emptyList(),
    /** Index into [EqualizerCapabilities.presetNames]; -1 means custom. */
    val presetIndex: Int = -1,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverbPreset: Int = 0,
    val loudnessGain: Int = 0,
    val skipSilence: Boolean = false
)

/**
 * Owns the platform audio effects attached to ExoPlayer's audio session.
 *
 * This is a singleton, and deliberately so. The effects need the player's
 * `audioSessionId`, which lives inside [MusicService]; the UI reaches the
 * player through a `MediaController`, and that API exposes none of this.
 * Because the service runs in the same process as the UI (no
 * `android:process` on it in the manifest), a shared object is a real,
 * working channel between them rather than a hack. If the service is ever
 * moved to its own process, this breaks and would have to be replaced with
 * custom `MediaSession` commands — hence the note here.
 *
 * Every effect is created defensively: audio effects are one of the least
 * reliable corners of the Android API and constructors routinely throw on
 * particular devices, ROMs, or when another app holds the session. A device
 * without a working equalizer should quietly show no equalizer, not crash.
 */
object AudioEngine {

    private var equalizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var reverbEffect: PresetReverb? = null
    private var loudnessEffect: LoudnessEnhancer? = null
    private var player: ExoPlayer? = null
    private var sessionId: Int? = null

    private val _capabilities = MutableStateFlow(EqualizerCapabilities())
    val capabilities: StateFlow<EqualizerCapabilities> = _capabilities

    /** The last settings applied, replayed whenever a new session attaches. */
    private var pending = AudioEffectSettings()

    /** Called by [MusicService] once the player exists. */
    fun attachPlayer(exoPlayer: ExoPlayer) {
        player = exoPlayer
        exoPlayer.skipSilenceEnabled = pending.skipSilence
    }

    /**
     * Binds the effects to [audioSessionId]. Safe to call repeatedly — the
     * session id changes over a player's life, and each change needs the
     * effects rebuilt against the new one.
     */
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == sessionId) return
        releaseEffects()
        sessionId = audioSessionId

        equalizer = runCatching { Equalizer(EFFECT_PRIORITY, audioSessionId) }.getOrNull()
        bassBoostEffect = runCatching { BassBoost(EFFECT_PRIORITY, audioSessionId) }.getOrNull()
        virtualizerEffect = runCatching { Virtualizer(EFFECT_PRIORITY, audioSessionId) }.getOrNull()
        reverbEffect = runCatching { PresetReverb(EFFECT_PRIORITY, audioSessionId) }.getOrNull()
        loudnessEffect = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()

        _capabilities.value = readCapabilities()
        applySettings(pending)
    }

    private fun readCapabilities(): EqualizerCapabilities {
        val eq = equalizer ?: return EqualizerCapabilities(available = false)
        return runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            EqualizerCapabilities(
                available = true,
                bandCount = bands,
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                // The platform reports centre frequencies in millihertz.
                centerFrequencies = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                presetNames = (0 until eq.numberOfPresets.toInt()).map { eq.getPresetName(it.toShort()) }
            )
        }.getOrElse { EqualizerCapabilities(available = false) }
    }

    /** Applies a full settings snapshot, ignoring anything unsupported. */
    fun applySettings(settings: AudioEffectSettings) {
        pending = settings
        player?.let { runCatching { it.skipSilenceEnabled = settings.skipSilence } }

        equalizer?.let { eq ->
            runCatching {
                eq.enabled = settings.equalizerEnabled
                if (settings.equalizerEnabled) {
                    if (settings.presetIndex >= 0 && settings.presetIndex < eq.numberOfPresets) {
                        eq.usePreset(settings.presetIndex.toShort())
                    } else {
                        settings.bandLevels.forEachIndexed { index, level ->
                            if (index < eq.numberOfBands) {
                                eq.setBandLevel(index.toShort(), level.toShort())
                            }
                        }
                    }
                }
            }
        }

        // The remaining effects are only enabled when they're actually doing
        // something — leaving a zero-strength effect attached still routes
        // audio through it, which costs battery for no audible benefit.
        bassBoostEffect?.let { effect ->
            runCatching {
                effect.enabled = settings.bassBoost > 0
                if (settings.bassBoost > 0) effect.setStrength(settings.bassBoost.toShort())
            }
        }
        virtualizerEffect?.let { effect ->
            runCatching {
                effect.enabled = settings.virtualizer > 0
                if (settings.virtualizer > 0) effect.setStrength(settings.virtualizer.toShort())
            }
        }
        reverbEffect?.let { effect ->
            runCatching {
                effect.enabled = settings.reverbPreset > 0
                if (settings.reverbPreset > 0) effect.preset = settings.reverbPreset.toShort()
            }
        }
        loudnessEffect?.let { effect ->
            runCatching {
                effect.enabled = settings.loudnessGain > 0
                if (settings.loudnessGain > 0) effect.setTargetGain(settings.loudnessGain)
            }
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoostEffect?.release() }
        runCatching { virtualizerEffect?.release() }
        runCatching { reverbEffect?.release() }
        runCatching { loudnessEffect?.release() }
        equalizer = null
        bassBoostEffect = null
        virtualizerEffect = null
        reverbEffect = null
        loudnessEffect = null
    }

    fun release() {
        releaseEffects()
        player = null
        sessionId = null
        _capabilities.value = EqualizerCapabilities()
    }
}

/** Above 0 so our effects win over other apps' on the same session. */
private const val EFFECT_PRIORITY = 1000

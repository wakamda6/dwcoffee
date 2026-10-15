package com.shilapi.xcertplay.hud

import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import com.shilapi.xcertplay.media.CarPlayPlaybackStatus

/** Vendor independent CarPlay control data for secondary HUD presentations. */
object CarPlayHudFeed {
    data class Media(val title: String, val artist: String, val playing: Boolean,
                     val elapsedMillis: Long?, val durationMillis: Long?)
    data class State(val media: Media? = null, val call: CarPlayCallCard? = null,
                     val lanes: List<Int> = emptyList(), val guidance: ClusterTurnGuidance? = null)

    private val callParser = CarPlayCallState()
    private val mediaParser = CarPlayPlaybackStatus()
    private var state = State()
    @Volatile private var listener: ((State) -> Unit)? = null

    fun current(): State = synchronized(this) { state }

    fun setListener(next: ((State) -> Unit)?) {
        listener = next
        next?.invoke(current())
    }

    fun setGuidance(guidance: ClusterTurnGuidance?) {
        val next = synchronized(this) {
            if (state.guidance == guidance) return
            state = state.copy(guidance = guidance)
            state
        }
        listener?.invoke(next)
    }

    fun onFrame(frame: Iap2Frame) {
        val next = synchronized(this) {
            state = when (frame.messageId) {
                0x5001 -> {
                    mediaParser.acceptUpdate(frame)
                    val media = mediaParser.nowPlaying
                    state.copy(media = media.title?.let {
                        Media(it, media.artist.orEmpty(), media.playing,
                            media.elapsedMillis, media.durationMillis)
                    })
                }
                CarPlayCallState.CALL_STATE_UPDATE -> {
                    callParser.accept(frame)
                    state.copy(call = callParser.current())
                }
                0x5204 -> state.copy(lanes = parseLanes(frame.payload))
                else -> return
            }
            state
        }
        listener?.invoke(next)
    }

    fun clear() {
        val next = synchronized(this) {
            mediaParser.clearAll()
            callParser.clear()
            state = State()
            state
        }
        listener?.invoke(next)
    }

    /** LaneGuidanceInfoUpdate TLV: each lane record holds an index and a recommended direction. */
    private fun parseLanes(bytes: ByteArray): List<Int> {
        val lanes = sortedMapOf<Int, Int>()
        var offset = 0
        while (offset + 4 <= bytes.size) {
            val size = u16(bytes, offset)
            if (size < 4 || offset + size > bytes.size) return emptyList()
            if (u16(bytes, offset + 2) == 2) {
                val end = offset + size
                var item = offset + 4
                var index = lanes.size
                var direction = 0
                while (item + 4 <= end) {
                    val itemSize = u16(bytes, item)
                    if (itemSize < 4 || item + itemSize > end) return emptyList()
                    val type = u16(bytes, item + 2)
                    if (type == 0 && itemSize >= 6) index = u16(bytes, item + 4)
                    if ((type == 2 || type == 3) && itemSize >= 6) direction = u16(bytes, item + 4)
                    item += itemSize
                }
                if (index in 0..7) lanes[index] = direction
            }
            offset += size
        }
        return lanes.values.take(8)
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 255) shl 8) or (bytes[offset + 1].toInt() and 255)
}

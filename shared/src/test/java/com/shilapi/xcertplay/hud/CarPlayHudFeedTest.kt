package com.shilapi.xcertplay.hud

import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import com.shilapi.xcertplay.iap2.message.Iap2Messages
import org.junit.Assert.assertEquals
import org.junit.Test

class CarPlayHudFeedTest {
    @Test fun mediaProgressRetainsIncrementalNowPlayingFields() {
        CarPlayHudFeed.clear()
        CarPlayHudFeed.onFrame(Iap2Messages.buildRaw(0x5001) {
            group(0) { string(1, "Track"); string(12, "Artist"); u32(4, 180_000) }
        })
        CarPlayHudFeed.onFrame(Iap2Messages.buildRaw(0x5001) {
            group(1) { u8(0, 1); u32(1, 45_000) }
        })
        assertEquals(CarPlayHudFeed.Media("Track", "Artist", true, 45_000, 180_000),
            CarPlayHudFeed.current().media)
        CarPlayHudFeed.clear()
    }

    @Test fun laneRecordsStayInIndexOrderAndMalformedUpdatesClearThem() {
        CarPlayHudFeed.clear()
        val lane2 = byteArrayOf(0, 16, 0, 2, 0, 6, 0, 0, 0, 2, 0, 6, 0, 3, 0, 21)
        val lane0 = byteArrayOf(0, 16, 0, 2, 0, 6, 0, 0, 0, 0, 0, 6, 0, 3, 0, 3)
        CarPlayHudFeed.onFrame(Iap2Frame(0x5204, lane2 + lane0))
        assertEquals(listOf(3, 21), CarPlayHudFeed.current().lanes)
        CarPlayHudFeed.onFrame(Iap2Frame(0x5204, byteArrayOf(0, 99, 0, 2)))
        assertEquals(emptyList<Int>(), CarPlayHudFeed.current().lanes)
        CarPlayHudFeed.clear()
    }
}

package com.wpdas.primetv.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    @Test fun `intervalos padrao e ciclo`() {
        assertEquals(SyncInterval.D1, SyncInterval.defaultFor(ContentSection.MOVIES))
        assertEquals(SyncInterval.H6, SyncInterval.defaultFor(ContentSection.EPG))
        assertEquals(SyncInterval.H6, SyncInterval.defaultFor(ContentSection.LIVE))
        assertEquals(SyncInterval.H12, SyncInterval.H6.next())
        assertEquals(SyncInterval.H6, SyncInterval.MANUAL.next())
        assertTrue(SyncInterval.MANUAL.isManual)
        assertEquals(24L * 60 * 60 * 1000, SyncInterval.D1.millis)
    }

    @Test fun `settings usa padrao quando a secao nao esta no mapa`() {
        val s = AppSettings(syncIntervals = mapOf(ContentSection.LIVE to SyncInterval.H6))
        assertEquals(SyncInterval.H6, s.interval(ContentSection.LIVE))
        assertEquals(SyncInterval.D1, s.interval(ContentSection.SERIES))
        assertEquals(BufferProfile.HIGH, BufferProfile.NORMAL.next())
    }
}

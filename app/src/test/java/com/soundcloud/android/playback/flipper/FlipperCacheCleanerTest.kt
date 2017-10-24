package com.soundcloud.android.playback.flipper

import android.content.SharedPreferences
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.playback.flipper.FlipperCacheCleaner.Result
import com.soundcloud.android.properties.FeatureFlags
import com.soundcloud.android.properties.Flag
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.rx.eventbus.TestEventBusV2
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@Suppress("IllegalIdentifier")
class FlipperCacheCleanerTest : AndroidUnitTest() {

    @Mock lateinit var featureFlags: FeatureFlags
    @Mock lateinit var sharedPreferences: SharedPreferences
    @Mock lateinit var flipperConfiguration: FlipperConfiguration
    @Mock lateinit var flipperCache: FlipperCache
    @Mock lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    val eventBus = TestEventBusV2()

    lateinit var flipperCacheCleaner: FlipperCacheCleaner

    @Before
    fun setUp() {
        flipperCacheCleaner = FlipperCacheCleaner(featureFlags, sharedPreferences, flipperConfiguration, eventBus)

        whenever(flipperConfiguration.cache).thenReturn(flipperCache)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(sharedPreferencesEditor)
        whenever(flipperCache.clearCache()).thenReturn(true)
    }

    @Test
    fun `do not clear cache if feature flag is not enabled`() {
        setUpConditions(featureFlagEnabled = false)

        flipperCacheCleaner.clearCacheIfNecessary()

        verifyZeroInteractions(flipperConfiguration)
        verifyZeroInteractions(sharedPreferences)
        eventBus.verifyNoEventsOn(EventQueue.TRACKING)
    }

    @Test
    fun `do not clear cache if the cache has already been cleared`() {
        setUpConditions(hasAlreadyBeenCleared = true)

        flipperCacheCleaner.clearCacheIfNecessary()

        verifyZeroInteractions(flipperConfiguration)
        eventBus.verifyNoEventsOn(EventQueue.TRACKING)
    }

    @Test
    fun `do not clear cache if the cache is empty`() {
        setUpConditions(cacheIsEmpty = true)

        flipperCacheCleaner.clearCacheIfNecessary()

        verify(flipperCache, never()).clearCache()
        verifySharedPreferencesMarkedWithCacheCleared()
        verifyResultLogged(Result.WAS_ALREADY_EMPTY)
    }

    @Test
    fun `does not clear cache if setting shared preferences failed`() {
        setUpConditions(setSharedPreferencesResult = false)

        flipperCacheCleaner.clearCacheIfNecessary()

        verify(flipperCache, never()).clearCache()
        verifySharedPreferencesMarkedWithCacheCleared()
        verifyResultLogged(Result.FAILED_TO_SET_SHARED_PREFERENCES)
    }

    @Test
    fun `clears the cache when the feature flag is enabled, the cache has never been cleared before, shared preferences was set, and the cache is not empty`() {
        setUpConditions(featureFlagEnabled = true, hasAlreadyBeenCleared = false, setSharedPreferencesResult = true, cacheIsEmpty = false)

        flipperCacheCleaner.clearCacheIfNecessary()

        verify(flipperCache).clearCache()
        verifySharedPreferencesMarkedWithCacheCleared()
        verifyResultLogged(Result.SUCCESS)
    }

    @Test
    fun `handles failure to clear the cache`() {
        setUpConditions(featureFlagEnabled = true, hasAlreadyBeenCleared = false, setSharedPreferencesResult = true, cacheIsEmpty = false)
        whenever(flipperCache.clearCache()).thenReturn(false)

        flipperCacheCleaner.clearCacheIfNecessary()

        verify(flipperCache).clearCache()
        verifySharedPreferencesMarkedWithCacheCleared()
        verifyResultLogged(Result.FAILURE)
    }

    private fun setUpConditions(featureFlagEnabled: Boolean = true,
                                hasAlreadyBeenCleared: Boolean = false,
                                setSharedPreferencesResult: Boolean = true,
                                cacheIsEmpty: Boolean = false) {
        whenever(featureFlags.isEnabled(Flag.CLEAR_FLIPPER_CACHE)).thenReturn(featureFlagEnabled)
        whenever(sharedPreferences.getBoolean(eq(FlipperCacheCleaner.HAS_ALREADY_CLEARED_FLIPPER_CACHE), anyBoolean())).thenReturn(hasAlreadyBeenCleared)
        whenever(sharedPreferencesEditor.commit()).thenReturn(setSharedPreferencesResult)
        whenever(flipperCache.isEmpty()).thenReturn(cacheIsEmpty)
    }

    private fun verifySharedPreferencesMarkedWithCacheCleared() {
        verify(sharedPreferencesEditor).putBoolean(FlipperCacheCleaner.HAS_ALREADY_CLEARED_FLIPPER_CACHE, true)
    }

    private fun verifyResultLogged(result: Result) {
        val trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING) as FlipperCacheCleaner.ClearFlipperCacheEvent
        val expectedTrackingEvent = FlipperCacheCleaner.ClearFlipperCacheEvent(result)
        assertEquals(expectedTrackingEvent.toMetric(), trackingEvent.toMetric())
    }
}

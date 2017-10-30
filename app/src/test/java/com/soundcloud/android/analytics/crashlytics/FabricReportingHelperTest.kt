package com.soundcloud.android.analytics.crashlytics

import android.content.SharedPreferences
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.settings.SettingKey
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock

@Suppress("IllegalIdentifier")
class FabricReportingHelperTest : AndroidUnitTest() {

    @Mock private lateinit var applicationProperties: ApplicationProperties
    @Mock private lateinit var sharedPreferences: SharedPreferences

    private lateinit var helper: FabricReportingHelper

    @Before
    fun setUp() {
        helper = FabricReportingHelper(applicationProperties, sharedPreferences)
    }

    @Test
    fun `returns true when the application is set up for crash reporting and the user has not disabled crash reporting`() {
        whenever(applicationProperties.shouldReportCrashes()).thenReturn(true)
        whenever(sharedPreferences.getBoolean(eq(SettingKey.CRASH_REPORTING_ENABLED), any())).thenReturn(true)

        assert(helper.isReportingCrashes())
    }

    @Test
    fun `returns false when the application is not set up for crash reporting`() {
        whenever(applicationProperties.shouldReportCrashes()).thenReturn(false)

        assertFalse(helper.isReportingCrashes())
    }

    @Test
    fun `returns false when the application is set up for crash reporting but the user has disabled crash reporting`() {
        whenever(applicationProperties.shouldReportCrashes()).thenReturn(true)
        whenever(sharedPreferences.getBoolean(eq(SettingKey.CRASH_REPORTING_ENABLED), any())).thenReturn(false)

        assertFalse(helper.isReportingCrashes())
    }
}

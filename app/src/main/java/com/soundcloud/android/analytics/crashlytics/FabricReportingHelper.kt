package com.soundcloud.android.analytics.crashlytics

import android.content.SharedPreferences
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.settings.SettingKey
import com.soundcloud.android.utils.OpenForTesting

@OpenForTesting
class FabricReportingHelper
constructor(val applicationProperties: ApplicationProperties,
            val sharedPreferences: SharedPreferences) {

    fun isReportingCrashes(): Boolean = applicationProperties.shouldReportCrashes() && sharedPreferences.getBoolean(SettingKey.CRASH_REPORTING_ENABLED, true)

}

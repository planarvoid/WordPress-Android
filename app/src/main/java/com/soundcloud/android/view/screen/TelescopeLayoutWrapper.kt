package com.soundcloud.android.view.screen

import android.support.v7.app.AppCompatActivity
import android.view.View
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.utils.BugReporter
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.view.CustomTelescopeLayout
import javax.inject.Inject

@OpenForTesting
class TelescopeLayoutWrapper
@Inject
constructor(val bugReporter: BugReporter) {

    fun wrapLayoutIfNecessary(activity: AppCompatActivity, layout: View): View {
        if (ApplicationProperties.isAlphaOrBelow()) {
            val customTelescopeLayout = CustomTelescopeLayout(activity, bugReporter)
            customTelescopeLayout.addView(layout)
            return customTelescopeLayout
        }
        return layout
    }
}

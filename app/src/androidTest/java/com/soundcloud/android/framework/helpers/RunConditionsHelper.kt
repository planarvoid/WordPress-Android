package com.soundcloud.android.framework.helpers

import android.support.test.InstrumentationRegistry
import com.soundcloud.android.properties.FeatureFlagsHelper
import com.soundcloud.android.properties.Flag
import org.junit.Assume

class RunConditionsHelper {
    private val featureFlags = FeatureFlagsHelper.create(InstrumentationRegistry.getTargetContext())

    private var shouldRunTest = true
    private var requiredEnabledFeatures: Array<Flag> = emptyArray()
    private var requiredDisabledFeatures: Array<Flag> = emptyArray()

    fun apply() {
        Assume.assumeTrue(shouldRunTest())
    }

    fun setRunCondition(runCondition: Boolean) {
        this.shouldRunTest = runCondition
    }

    fun requiredEnabledFeatures(requiredEnabledFeatures: Array<Flag>) {
        this.requiredEnabledFeatures = requiredEnabledFeatures
    }

    fun requireDisabledFeatures(requiredDisabledFeatures: Array<Flag>) {
        this.requiredDisabledFeatures = requiredDisabledFeatures
    }

    private fun shouldRunTest() = shouldRunTest && requiredEnabledFeaturesAreEnabled() && requiredDisabledFeaturesAreDisabled()
    private fun requiredEnabledFeaturesAreEnabled() = featureFlags.isLocallyEnabled(requiredDisabledFeatures)
    private fun requiredDisabledFeaturesAreDisabled() = featureFlags.isLocallyDisabled(requiredDisabledFeatures)
}

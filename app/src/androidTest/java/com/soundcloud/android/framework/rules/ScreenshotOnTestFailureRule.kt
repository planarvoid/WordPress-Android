package com.soundcloud.android.framework.rules

import android.app.Activity
import android.support.test.InstrumentationRegistry
import com.google.android.libraries.cloudtesting.screenshots.ScreenShotter
import com.soundcloud.android.framework.Han
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Suppress("detekt.RethrowCaughtException", "detekt.TooGenericExceptionCaught")
class ScreenshotOnTestFailureRule : TestRule {
    var activity: Activity? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    val solo = Han(InstrumentationRegistry.getInstrumentation())
                    solo.takeScreenshot(description.displayName)
                    activity?.let {
                        ScreenShotter.takeScreenshot(description.displayName, it)
                    }
                    println("Boom! Screenshot! - Captured screenshot for failed test: ${description.displayName}")
                    throw t
                } finally {
                    activity = null
                }
            }
        }
    }

    fun start(activity: Activity) {
        this.activity = activity
    }
}


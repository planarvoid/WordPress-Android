package com.soundcloud.android.framework.rules

import android.support.test.InstrumentationRegistry
import com.soundcloud.android.framework.Han
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ScreenshotOnTestFailureRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    val solo = Han(InstrumentationRegistry.getInstrumentation())
                    solo.takeScreenshot(description.displayName)
                    println("Boom! Screenshot! - Captured screenshot for failed test: ${description.displayName}")
                    throw t
                }
            }
        }
    }
}


package com.soundcloud.android.framework.rules

import android.support.test.InstrumentationRegistry
import android.util.Log
import com.soundcloud.android.framework.LogCollector
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class LogHandlerRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val testCaseName = String.format("%s.%s", javaClass.name, description.methodName)
                try {
                    // setUp
                    LogCollector.startCollecting(InstrumentationRegistry.getTargetContext(), testCaseName)
                    Log.d("TESTSTART:", String.format("%s", testCaseName))

                    base.evaluate()
                    // success
                    LogCollector.markFileForDeletion()
                } finally {
                    // tearDown
                    Log.d("TESTEND:", String.format("%s", testCaseName))
                    LogCollector.stopCollecting()
                }
            }
        }
    }
}

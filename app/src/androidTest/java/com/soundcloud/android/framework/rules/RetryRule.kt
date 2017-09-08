package com.soundcloud.android.framework.rules

import com.soundcloud.android.utils.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule that will retry tests in case they fail.
 * The given retry count indicates how often a test will be retried.
 * A retryCount of 0 means the test will only run once and won't be retried
 */
class RetryRule(private val retryCount: Int) : TestRule {
    private val actualRuns = retryCount + 1

    override fun apply(base: Statement, description: Description): Statement {
        return object: Statement() {
            override fun evaluate() {
                val throwableList = mutableListOf<Throwable>()

                (0..retryCount).forEach {
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        throwableList.add(t)
                        Log.e("TESTRUNNER", "Run (${it + 1}/$actualRuns): ${description.displayName} failed with $t")
                    }
                }

                Log.e("TESTRUNNER", "${description.displayName} failed after $actualRuns run(s).")
                throw throwableList.last();
            }
        }
    }
}

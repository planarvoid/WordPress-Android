package com.soundcloud.android.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

// http://stackoverflow.com/questions/1492856/easy-way-of-running-the-same-junit-test-over-and-over
// Use this rule to debug flaky tests.
//
// Add: `@Rule public RepeatRule repeatRule = new RepeatRule();` to the test
// Annotate with `@Repeat(10)` the flaky test.
public class RepeatRule implements TestRule {

    private static class RepeatStatement extends Statement {
        private final Statement statement;
        private final int repeat;

        public RepeatStatement(Statement statement, int repeat) {
            this.statement = statement;
            this.repeat = repeat;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < repeat; i++) {
                statement.evaluate();
            }
        }

    }

    @Override
    public Statement apply(Statement statement, Description description) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation(Repeat.class);
        if (repeat != null) {
            int times = repeat.value();
            result = new RepeatStatement(statement, times);
        }
        return result;
    }
}

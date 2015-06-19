package com.soundcloud.android.testsupport;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

/**
 * Proxy class that test cases may inherit from whenever the test subject is derived
 * form a platform component (such as Fragments or Adapters.)
 * If all platform references are merely dependencies, prefer a plain JUnit test where
 * platform components are mocked out.
 * <p/>
 * KEEP FREE OF TEST FRAMEWORK SPECIFIC REFERENCES.
 */
public abstract class PlatformUnitTest extends RobolectricUnitTest {

    @Rule public TestRule injectMocksRule = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            MockitoAnnotations.initMocks(PlatformUnitTest.this);
            return base;
        }
    };
}

package com.soundcloud.android.rx;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Test;

public class OperationDurationLoggerTest {

    @Test
    public void createPicksTheFirstCallSite() {
        final StackTraceElement subscriptionInApp = create("com.soundcloud.android.TestClass", "methodName");
        final StackTraceElement[] traceElements = new StackTraceElement[] {
                subscriptionInApp,
                create("com.soundcloud.android.TestClass2", "methodName")
        };

        final OperationDurationLogger.TimeMeasure measure = OperationDurationLogger.create(traceElements, new TestDateProvider());
        assertThat(measure.name()).isEqualTo(subscriptionInApp.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createThrowsWhenCallSiteIsEmpty() {
        OperationDurationLogger.create(new StackTraceElement[0], new TestDateProvider());
    }

    @Test
    public void createIgnoresRxPackage() {
        final StackTraceElement subscriptionInApp = create("com.soundcloud.android.TestClass", "methodName");
        final StackTraceElement[] traceElements = new StackTraceElement[] {
                create("com.soundcloud.android.rx.TestClass2", "methodName"),
                subscriptionInApp
        };

        final OperationDurationLogger.TimeMeasure measure = OperationDurationLogger.create(traceElements, new TestDateProvider());
        assertThat(measure.name()).isEqualTo(subscriptionInApp.toString());
    }

    @Test
    public void createIgnoresUnknownCallSites() {
        final StackTraceElement subscriptionInApp = create("com.soundcloud.android.TestClass", "methodName");
        final StackTraceElement[] traceElements = new StackTraceElement[] {
                create("com.something.TestClass2", "methodName"),
                subscriptionInApp
        };

        final OperationDurationLogger.TimeMeasure measure = OperationDurationLogger.create(traceElements, new TestDateProvider());
        assertThat(measure.name()).isEqualTo(subscriptionInApp.toString());
    }

    @Test
    public void createTakeFirstCallSiteWhenNoneIsKnown() {
        final StackTraceElement firstCallSite = create("com.something.TestClass", "methodName");
        final StackTraceElement[] traceElements = new StackTraceElement[] {
                firstCallSite,
                create("com.something.TestClass2", "methodName")
        };

        final OperationDurationLogger.TimeMeasure measure = OperationDurationLogger.create(traceElements, new TestDateProvider());
        assertThat(measure.name()).isEqualTo(firstCallSite.toString());
    }

    private StackTraceElement create(String aClass, String methodName) {
        return new StackTraceElement(aClass, methodName, "fileName.java", 42);
    }
}

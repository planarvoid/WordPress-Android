package com.soundcloud.android.testsupport.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.java.checks.Preconditions;
import org.assertj.core.api.AbstractAssert;

import android.os.Bundle;


public class PerformanceMetricAssert extends AbstractAssert<PerformanceMetricAssert, PerformanceMetric> {

    private final Bundle metricParamsBundle;

    public PerformanceMetricAssert(PerformanceMetric actual) {
        super(actual, PerformanceMetricAssert.class);

        metricParamsBundle = actual.metricParams().toBundle();
    }

    public PerformanceMetricAssert hasMetricType(MetricType metricType) {
        Preconditions.checkNotNull(metricType);
        isNotNull();
        assertThat(actual.metricType())
                .isEqualTo(metricType);
        return this;
    }

    public PerformanceMetricAssert containsMetricParam(MetricKey metricKey, String value) {
        Preconditions.checkNotNull(metricKey);
        isNotNull();
        checkContainsKey(metricKey);
        assertThat(metricParamsBundle.getString(metricKey.toString())).isEqualTo(value);
        return this;
    }

    public PerformanceMetricAssert containsMetricParam(MetricKey metricKey, long value) {
        Preconditions.checkNotNull(metricKey);
        isNotNull();
        checkContainsKey(metricKey);
        assertThat(metricParamsBundle.getLong(metricKey.toString())).isEqualTo(value);
        return this;
    }

    public PerformanceMetricAssert containsMetricParam(MetricKey metricKey, boolean value) {
        Preconditions.checkNotNull(metricKey);
        isNotNull();
        checkContainsKey(metricKey);
        assertThat(metricParamsBundle.getBoolean(metricKey.toString())).isEqualTo(value);
        return this;
    }

    private void checkContainsKey(MetricKey metricKey) {
        assertThat(metricParamsBundle.containsKey(metricKey.toString()))
                .overridingErrorMessage("Given PerformanceMetric does not contain any param with key %s", metricKey.toString())
                .isTrue();
    }
}

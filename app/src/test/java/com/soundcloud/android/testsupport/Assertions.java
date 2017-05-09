package com.soundcloud.android.testsupport;

import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.testsupport.assertions.ActivityAssert;
import com.soundcloud.android.testsupport.assertions.AndroidUriAssert;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.android.testsupport.assertions.PerformanceMetricAssert;
import com.soundcloud.android.testsupport.assertions.ServiceAssert;
import com.soundcloud.android.testsupport.assertions.TextViewAssert;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

/**
 * Entry point for all custom assertions.
 */
public class Assertions {

    private Assertions() {
    }

    public static ServiceAssert assertThat(Service service) {
        return new ServiceAssert(service);
    }

    public static ActivityAssert assertThat(Activity activityContext) {
        return new ActivityAssert(activityContext);
    }

    public static TextViewAssert assertThat(TextView textView) {
        return new TextViewAssert(textView);
    }

    public static IntentAssert assertThat(Intent intent) {
        return new IntentAssert(intent);
    }

    public static PerformanceMetricAssert assertThat(PerformanceMetric performanceMetric) {
        return new PerformanceMetricAssert(performanceMetric);
    }

    public static AndroidUriAssert assertThat(Uri uri) {
        return new AndroidUriAssert(uri);
    }
}

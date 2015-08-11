package com.soundcloud.android.testsupport;

import com.soundcloud.android.testsupport.assertions.ActivityAssert;
import com.soundcloud.android.testsupport.assertions.ServiceAssert;
import com.soundcloud.android.testsupport.assertions.TextViewAssert;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import android.widget.TextView;

/**
 * Entry point for all custom assertions.
 */
public class Assertions {

    private Assertions() {}

    public static ServiceAssert assertThat(Service service) {
        return new ServiceAssert(service);
    }

    public static ActivityAssert assertThat(Activity activityContext) {
        return new ActivityAssert(activityContext);
    }

    public static TextViewAssert assertThat(TextView textView) {
        return new TextViewAssert(textView);
    }
}

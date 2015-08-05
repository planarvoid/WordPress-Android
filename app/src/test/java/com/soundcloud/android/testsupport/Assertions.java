package com.soundcloud.android.testsupport;

import com.soundcloud.android.testsupport.assertions.ActivityAssert;
import com.soundcloud.android.testsupport.assertions.ServiceAssert;

import android.app.Activity;
import android.app.Service;

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
}

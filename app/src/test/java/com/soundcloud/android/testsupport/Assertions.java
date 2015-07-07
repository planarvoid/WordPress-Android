package com.soundcloud.android.testsupport;

import com.soundcloud.android.testsupport.assertions.ServiceAssert;

import android.app.Service;

/**
 * Entry point for all custom assertions.
 */
public class Assertions {

    private Assertions() {}

    public static ServiceAssert assertThat(Service service) {
        return new ServiceAssert(service);
    }
}

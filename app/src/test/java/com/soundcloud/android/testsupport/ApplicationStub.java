package com.soundcloud.android.testsupport;

import android.app.Application;

/**
 * Used in Robolectric tests so that we don't go through our untestable Application#onCreate
 */
public class ApplicationStub extends Application {
}

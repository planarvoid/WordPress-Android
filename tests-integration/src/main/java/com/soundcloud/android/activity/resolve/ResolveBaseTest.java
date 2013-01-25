package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.activity.ResolveActivity;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

import android.content.Intent;
import android.net.Uri;

public abstract class ResolveBaseTest extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;

    public ResolveBaseTest() {
        super(ResolveActivity.class);
    }

    protected abstract Uri getUri();

    @Override
    protected void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        super.setUp();
    }

}

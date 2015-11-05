package com.soundcloud.android.tests.search.intents;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.search.LegacySearchActivity;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;

public abstract class SearchIntentsBaseTest extends ActivityTest<LegacySearchActivity> {

    public SearchIntentsBaseTest() {
        super(LegacySearchActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(getIntent());
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Intent getIntent();

}

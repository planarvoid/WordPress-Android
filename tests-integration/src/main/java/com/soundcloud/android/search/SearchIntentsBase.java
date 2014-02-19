package com.soundcloud.android.search;

import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;

public abstract class SearchIntentsBase extends ActivityTestCase<CombinedSearchActivity> {

    public SearchIntentsBase() {
        super(CombinedSearchActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        setActivityIntent(getIntent());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
        waiter = new Waiter(solo);
    }

    protected abstract Intent getIntent();

}

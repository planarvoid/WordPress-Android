package com.soundcloud.android.tests.search.intents;

import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.framework.AccountAssistant;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.Waiter;

import android.content.Intent;

public abstract class SearchIntentsBaseTest extends ActivityTest<SearchActivity> {

    public SearchIntentsBaseTest() {
        super(SearchActivity.class);
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

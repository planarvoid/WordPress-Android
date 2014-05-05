package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.TestUser.defaultUser;

import com.soundcloud.android.main.ResolveActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

import android.content.Intent;
import android.net.Uri;

public abstract class ResolveBaseTest extends ActivityTestCase<ResolveActivity> {
    protected static final int DEFAULT_WAIT = 30 * 1000;
    protected static LegacyPlayerScreen playerScreen;
    protected static ProfileScreen profileScreen;
    protected static Waiter waiter;

    public ResolveBaseTest() {
        super(ResolveActivity.class);
    }

    protected abstract Uri getUri();

    @Override
    protected void setUp() throws Exception {
        defaultUser.logIn(getInstrumentation().getTargetContext());
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
        waiter = new Waiter(solo);
    }
}

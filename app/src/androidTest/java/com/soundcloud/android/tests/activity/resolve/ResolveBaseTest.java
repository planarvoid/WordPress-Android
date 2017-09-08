package com.soundcloud.android.tests.activity.resolve;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public abstract class ResolveBaseTest extends ActivityTest<ResolveActivity> {

    public ResolveBaseTest() {
        super(ResolveActivity.class);
    }

    protected abstract Uri getUri();

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.resolverUser;
    }

    @Override
    public void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        super.setUp();
    }

    public VisualPlayerElement getPlayerElement() {
        return new VisualPlayerElement(solo).waitForExpandedPlayer();
    }
}

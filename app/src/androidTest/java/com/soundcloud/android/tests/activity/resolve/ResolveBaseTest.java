package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.TestUser.defaultUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
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
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        super.setUp();
        waiter.waitFiveSeconds();
    }

    public VisualPlayerElement getPlayerElement() {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }
}

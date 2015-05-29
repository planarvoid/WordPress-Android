package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.TestUser.emptyUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;
import android.net.Uri;

public class OtherProfileEmptyTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen screen;

    public OtherProfileEmptyTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        // cheap deeplink to the empty user
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/scEmpty")));
        super.setUp();
        screen = new ProfileScreen(solo);
    }

    public void testShowsEmptyPostsView() {
        final ViewElement emptyUserPostsMessage = screen
                .emptyUserPostsMessage(emptyUser.getPermalink());
        assertTrue(emptyUserPostsMessage.isVisible());
    }

    // ignore until we refactor the fragment
    public void ignore_testShowsEmptyLikesViewView() {
        final ViewElement emptyUserLikesMessage = screen
                .touchLikesTab()
                .emptyUserLikesMessage(emptyUser.getPermalink());

        assertTrue(emptyUserLikesMessage.isVisible());
    }

}

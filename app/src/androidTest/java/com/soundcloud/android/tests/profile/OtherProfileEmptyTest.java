package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.deeplinks.ResolveActivity;
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
        assertThat("This user hasn't uploaded\nany public sounds yet.", is(screen.emptyViewMessage()));
    }

    // ignore until we refactor the fragment
    public void ignore_testShowsEmptyLikesViewView() {
        ProfileScreen profileScreen = screen.touchLikesTab();

        assertThat("This user has no likes.", is(profileScreen.emptyViewMessage()));
    }

}

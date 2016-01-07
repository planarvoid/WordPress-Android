package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.R;
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
        playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        // cheap deeplink to the empty user
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/scEmpty")));
        super.setUp();
        screen = new ProfileScreen(solo);
    }

    public void testShowsEmptyPostsView() {
        assertThat(solo.getString(R.string.new_empty_user_posts_message), is(screen.emptyViewMessage()));
    }

    public void testShowsEmptyLikesViewView() {
        ProfileScreen profileScreen = screen.touchLikesTab();

        assertThat(solo.getString(R.string.new_empty_user_likes_text), is(profileScreen.emptyViewMessage()));
    }

}

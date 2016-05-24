package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.annotation.NewProfileTest;
import com.soundcloud.android.framework.annotation.ProfileAlbumTest;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

@NewProfileTest
@ProfileAlbumTest
public class OtherProfileAlbumTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen profileScreen;

    public OtherProfileAlbumTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.OTHER_PROFILE_ALBUM_USER_URI));
        super.setUp();

        profileScreen = new ProfileScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testHasAlbumsBucket() {
        assertTrue(profileScreen.albumsHeader().hasVisibility());
    }

    public void testViewAllAlbums() {
        profileScreen.clickViewAllAlbums();
        assertEquals(profileScreen.getActionBarTitle(),
                ressourceString(R.string.user_profile_sounds_header_albums));
    }
}

package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolveSetMobileUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertThat(pd.getTitle(), is(equalTo("Ecclesia Inspiration")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SET_M_URI;
    }
}

package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolvePlaylistNormalUrlTest extends ResolveBaseTest {

    public void ignoreShouldOpenPlaylistDetails() throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertThat(pd.getTitle(), is(equalTo("Ecclesia Inspiration")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_URI;
    }

}

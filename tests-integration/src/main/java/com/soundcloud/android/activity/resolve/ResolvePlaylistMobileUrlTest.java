package com.soundcloud.android.activity.resolve;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolvePlaylistMobileUrlTest extends ResolveBaseTest {

    public void ignoreShouldOpenPlaylistDetailsScreen () throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertThat(pd.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }

    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_M_URI;
    }
}

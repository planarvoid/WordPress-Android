package com.soundcloud.android.stream;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

public class HeaderSpannableBuilderTest extends AndroidUnitTest {

    private final String USERNAME = "username";
    private final String ACTION = "posted";

    @Mock StreamItemViewHolder trackView;
    private HeaderSpannableBuilder builder;

    @Before
    public void setUp() throws Exception {
        when(trackView.getContext()).thenReturn(context());
        builder = new HeaderSpannableBuilder(resources());
    }

    @Test
    public void buildsUserActionStringForATrack() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, TrackItem.PLAYABLE_TYPE)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a track");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForAPlaylist() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, PlaylistItem.TYPE_PLAYLIST)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a playlist");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForAnAlbum() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, PlaylistItem.TYPE_ALBUM)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted an album");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForAnEp() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, PlaylistItem.TYPE_EP)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted an EP");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForACompilation() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, PlaylistItem.TYPE_COMPILATION)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a compilation");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForASingle() {
        final SpannableString spannableString = builder
                .userActionSpannedString(USERNAME, ACTION, PlaylistItem.TYPE_SINGLE)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a single");
        assertThatHasForegroundSpan(spannableString);
    }

    private void assertThatHasForegroundSpan(SpannableString spanned) {
        assertThat(spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class)).isNotEmpty();
    }
}

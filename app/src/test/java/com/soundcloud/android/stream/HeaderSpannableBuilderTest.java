package com.soundcloud.android.stream;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

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
                .trackUserAction(USERNAME, ACTION)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a track");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsUserActionStringForAPlaylist() {
        final SpannableString spannableString = builder
                .playlistUserAction(USERNAME, ACTION)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a playlist");
        assertThatHasForegroundSpan(spannableString);
    }

    @Test
    public void buildsSpannableStringHeaderWithIcon(){
        final SpannableString spannableString = builder
                .playlistUserAction(USERNAME, ACTION)
                .withIconSpan(trackView)
                .get();

        assertThat(spannableString.toString()).isEqualTo("username posted a playlist");
        assertThatHasForegroundSpan(spannableString);
        assertThatHasImageSpan(spannableString);
    }

    private void assertThatHasImageSpan(SpannableString spanned) {
        assertThat(spanned.getSpans(0, spanned.length(), ImageSpan.class)).isNotEmpty();
    }

    private void assertThatHasForegroundSpan(SpannableString spanned) {
        assertThat(spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class)).isNotEmpty();
    }
}
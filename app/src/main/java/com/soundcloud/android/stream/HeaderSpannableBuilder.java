package com.soundcloud.android.stream;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.appboy.ui.support.StringUtils;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import javax.inject.Inject;

class HeaderSpannableBuilder {

    private final Resources resources;

    private SpannableString spannedString;

    @Inject
    HeaderSpannableBuilder(Resources resources) {
        this.resources = resources;
    }

    SpannableString get() {
        return spannedString;
    }

    HeaderSpannableBuilder promotedSpannedString(boolean isTrack) {
        final String playable = playableString(isTrack);
        final String headerText = resources.getString(R.string.stream_promoted_playable, playable);
        final int spanEnd = headerText.length() - playable.length();

        return createSpannedString(headerText, 0, spanEnd);
    }

    HeaderSpannableBuilder actionSpannedString(String action, boolean isTrack) {
        final String headerText = resources.getString(userActionTextId(isTrack), StringUtils.EMPTY_STRING, action);
        final int spanEnd = action.length() + 1;

        createSpannedString(headerText, 0, spanEnd);
        return this;
    }

    HeaderSpannableBuilder userActionSpannedString(String user, String action, boolean isTrack) {
        final String headerText = resources.getString(userActionTextId(isTrack), user, action);
        final int spanEnd = user.length() + action.length() + 1;

        createSpannedString(headerText, user.length(), spanEnd);
        return this;
    }

    private HeaderSpannableBuilder createSpannedString(String headerText, int spanStart, int spanEnd) {
        spannedString = new SpannableString(headerText);
        spannedString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                spanStart,
                spanEnd,
                SPAN_EXCLUSIVE_EXCLUSIVE);
        return this;
    }

    private int userActionTextId(boolean isTrack) {
        return isTrack ? R.string.stream_track_header_text : R.string.stream_playlist_header_text;
    }

    private String playableString(boolean isTrack) {
        return resources.getString(isTrack ? R.string.stream_track : R.string.stream_playlist);
    }
}

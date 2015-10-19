package com.soundcloud.android.stream;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
import static android.text.style.DynamicDrawableSpan.ALIGN_BOTTOM;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

import javax.inject.Inject;

class HeaderSpannableBuilder {

    private final Resources resources;

    private SpannableString spannedString;
    private int spanStart;

    @Inject
    HeaderSpannableBuilder(Resources resources) {
        this.resources = resources;
    }

    SpannableString get() {
        return spannedString;
    }

    HeaderSpannableBuilder playlistUserAction(String userName, String action) {
        return userActionSpannedString(userName, action, false);
    }

    HeaderSpannableBuilder trackUserAction(String userName, String action) {
        return userActionSpannedString(userName, action, true);
    }

    HeaderSpannableBuilder withIconSpan(StreamItemViewHolder trackView) {
        spannedString.setSpan(new ImageSpan(trackView.getContext(), R.drawable.stats_repost, ALIGN_BOTTOM),
                spanStart,
                spanStart + 1,
                SPAN_INCLUSIVE_EXCLUSIVE);
        return this;
    }

    private HeaderSpannableBuilder userActionSpannedString(String userName, String action, boolean isTrack) {
        spanStart = userName.length();

        final String headerText = resources.getString(headerTextResId(isTrack), userName, action);
        final int spanEnd = spanStart + action.length() + 1;

        spannedString = new SpannableString(headerText);
        spannedString.setSpan(new ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                spanStart,
                spanEnd,
                SPAN_EXCLUSIVE_EXCLUSIVE);
        return this;
    }

    private int headerTextResId(boolean isTrack) {
        return isTrack ? R.string.stream_track_header_text : R.string.stream_playlist_header_text;
    }
}

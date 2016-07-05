package com.soundcloud.android.stream;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;

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

    HeaderSpannableBuilder promotedSpannedString(String playableType) {
        final String playable = resources.getString(playableStringResource(playableType));
        final String headerText = resources.getString(R.string.stream_promoted_playable, playable);
        final int spanEnd = headerText.length() - playable.length();

        return createSpannedString(headerText, 0, spanEnd);
    }

    HeaderSpannableBuilder actionSpannedString(String action, String playableType) {
        final String headerText = resources.getString(userActionTextId(playableType), Strings.EMPTY, action);
        final int spanEnd = action.length() + 1;

        createSpannedString(headerText, 0, spanEnd);
        return this;
    }

    HeaderSpannableBuilder userActionSpannedString(String user, String action, String playableType) {
        final String headerText = resources.getString(userActionTextId(playableType), user, action);
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

    private int userActionTextId(String playableType) {
        switch (playableType) {
            case TrackItem.PLAYABLE_TYPE:
                return R.string.stream_track_header_text;
            case PlaylistItem.TYPE_PLAYLIST:
                return R.string.stream_playlist_header_text;
            case PlaylistItem.TYPE_ALBUM:
                return R.string.stream_album_header_text;
            case PlaylistItem.TYPE_EP:
                return R.string.stream_ep_header_text;
            case PlaylistItem.TYPE_SINGLE:
                return R.string.stream_single_header_text;
            case PlaylistItem.TYPE_COMPILATION:
                return R.string.stream_compilation_header_text;
            default:
                return R.string.stream_track_header_text;
        }
    }

    private int playableStringResource(String playableType) {
        switch (playableType) {
            case TrackItem.PLAYABLE_TYPE:
                return R.string.stream_track;
            default:
                return PlaylistItem.getSetTypeLabel(playableType);
        }
    }
}

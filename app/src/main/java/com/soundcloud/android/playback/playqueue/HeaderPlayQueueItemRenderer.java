package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.QueueUtils.getAlpha;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class HeaderPlayQueueItemRenderer implements CellRenderer<HeaderPlayQueueUIItem> {

    private final Resources resources;

    @Inject
    public HeaderPlayQueueItemRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_header_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<HeaderPlayQueueUIItem> items) {
        final HeaderPlayQueueUIItem item = items.get(position);
        final TextView textHolder = (TextView) itemView.findViewById(R.id.title);
        textHolder.setText(getTitle(item));
        itemView.setAlpha(getAlpha(item.getRepeatMode(), item.getPlayState()));
    }

    private String getTitle(HeaderPlayQueueUIItem item) {
        final PlaybackContext.Bucket bucket = item.getPlaybackContext().bucket();
        final String query = item.getPlaybackContext().query().or(Strings.EMPTY);
        final String contentTitle = item.getContentTitle().or(Strings.EMPTY);

        switch (bucket) {
            case SEARCH_RESULT:
                return resources.getString(R.string.play_queue_header_search, query);
            case STREAM:
                return resources.getString(R.string.play_queue_header_stream);
            case LINK:
                return resources.getString(R.string.play_queue_header_link);
            case PROFILE:
                return resources.getString(R.string.play_queue_header_profile, contentTitle);
            case PLAYLIST:
                return resources.getString(R.string.play_queue_header_playlist, contentTitle);
            case TRACK_STATION:
                return resources.getString(R.string.play_queue_header_track_station, contentTitle);
            case ARTIST_STATION:
                return resources.getString(R.string.play_queue_header_artist_station, contentTitle);
            case YOUR_LIKES:
                return resources.getString(R.string.play_queue_header_likes);
            case LISTENING_HISTORY:
                return resources.getString(R.string.play_queue_header_listening_history);
            case SUGGESTED_TRACKS:
                return resources.getString(R.string.play_queue_header_suggested_tracks);
            case CHARTS_TOP:
                return resources.getString(R.string.play_queue_header_charts_top, contentTitle);
            case CHARTS_TRENDING:
                return resources.getString(R.string.play_queue_header_charts_trending, contentTitle);
            case EXPLICIT:
                return resources.getString(R.string.play_queue_header_explicit);
            case AUTO_PLAY:
                return resources.getString(R.string.play_queue_header_auto_play);
            default:
                throw new IllegalArgumentException("can't render header of type: " + bucket.name());
        }
    }

}

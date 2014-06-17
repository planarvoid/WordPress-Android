package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class TrackItemPresenter implements CellPresenter<PropertySet> {

    private final LayoutInflater layoutInflater;

    private TrackUrn playingTrack = Urn.forTrack(ScModel.NOT_SET);

    @Inject
    public TrackItemPresenter(LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.track_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        final PropertySet track = trackItems.get(position);
        getTextView(itemView, R.id.title).setText(track.get(PlayableProperty.TITLE));
        getTextView(itemView, R.id.username).setText(track.get(PlayableProperty.CREATOR));
        final String formattedDuration = ScTextUtils.formatTimestamp(track.get(PlayableProperty.DURATION));
        getTextView(itemView, R.id.duration).setText(formattedDuration);

        togglePlayCountOrNowPlaying(itemView, track);
        toggleReposterView(itemView, track);
    }

    private void toggleReposterView(View itemView, PropertySet track) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        if (track.contains(PlayableProperty.REPOSTER)) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(track.get(PlayableProperty.REPOSTER));
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void togglePlayCountOrNowPlaying(View itemView, PropertySet track) {
        final TextView playCountText = getTextView(itemView, R.id.play_count);
        final TextView nowPlayingText = getTextView(itemView, R.id.now_playing);
        if (track.get(PlayableProperty.URN).equals(playingTrack)) {
            playCountText.setVisibility(View.GONE);
            nowPlayingText.setVisibility(View.VISIBLE);
        } else {
            nowPlayingText.setVisibility(View.GONE);
            playCountText.setVisibility(View.VISIBLE);
            playCountText.setText(Long.toString(track.get(TrackProperty.PLAY_COUNT)));
        }
    }

    public void setPlayingTrack(@NotNull TrackUrn playingTrack) {
        this.playingTrack = playingTrack;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

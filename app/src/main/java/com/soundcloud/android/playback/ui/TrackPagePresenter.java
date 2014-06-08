package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackPagePresenter implements View.OnClickListener {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final WaveformOperations waveformOperations;
    private final Listener listener;

    interface Listener {
        void onTogglePlay();
        void onNext();
        void onPrevious();
        void onFooterTap();
        void onPlayerClose();
    }

    @Inject
    public TrackPagePresenter(Resources resources, ImageOperations imageOperations,
                              WaveformOperations waveformOperations, TrackPageListener listener) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.waveformOperations = waveformOperations;
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_play:
            case R.id.track_page_artwork:
                listener.onTogglePlay();
                break;
            case R.id.track_page_next:
                listener.onNext();
                break;
            case R.id.track_page_previous:
                listener.onPrevious();
                break;
            case R.id.footer_controls:
                listener.onFooterTap();
                break;
            case R.id.player_close:
            case R.id.player_bottom_close:
                listener.onPlayerClose();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    public View createTrackPage(ViewGroup container, boolean fullScreen) {
        View trackView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        setFullScreen(trackView, fullScreen);
        return trackView;
    }

    public void setProgress(View trackView, PlaybackProgressEvent progress) {
        getViewHolder(trackView).artwork.setProgressProportion(progress.getProgressProportion());
    }

    public void resetProgress(View trackView) {
        setProgress(trackView, PlaybackProgressEvent.empty());
    }

    public void setTrackPlayState(View trackView, boolean isPlaying) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footerPlayToggle.setChecked(isPlaying);
    }

    public void setGlobalPlayState(View trackView, boolean isPlaying) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.waveform.setVisibility(isPlaying ? View.VISIBLE : View.GONE);

        final int playControlVisibility = isPlaying ? View.GONE : View.VISIBLE;
        holder.nextButton.setVisibility(playControlVisibility);
        holder.previousButton.setVisibility(playControlVisibility);
        holder.playButton.setVisibility(playControlVisibility);
    }

    public void setFullScreen(View trackView, boolean expanded) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(expanded ? View.GONE : View.VISIBLE);

        final int fullScreenVisibility = expanded ? View.VISIBLE : View.GONE;
        holder.user.setVisibility(fullScreenVisibility);
        holder.title.setVisibility(fullScreenVisibility);
        holder.close.setVisibility(fullScreenVisibility);
    }

    public void populateTrackPage(View trackView, Track track) {
        populateTrackPage(trackView, track, 0);
    }

    public void populateTrackPage(View trackView, Track track, PlaybackProgressEvent currentProgress) {
        populateTrackPage(trackView, track, currentProgress == null ? 0 : currentProgress.getProgressProportion());
    }

    private void populateTrackPage(View trackView, Track track, float currentProgressProportion) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.user.setText(track.getUserName());
        holder.title.setText(track.getTitle());
        imageOperations.displayInVisualPlayer(track.getUrn(), ApiImageSize.getFullImageSize(resources), holder.artwork);
        holder.artwork.setProgressProportion(currentProgressProportion);
        waveformOperations.display(track.getUrn(), track.getWaveformUrl(), holder.waveform);

        holder.footerPlayToggle.setChecked(false); // Reset to paused state
        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());

        for (View v : holder.getOnClickViews()) {
            v.setOnClickListener(this);
        }
    }

    private TrackPageHolder getViewHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        TrackPageHolder holder = new TrackPageHolder();
        holder.title = (JaggedTextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (JaggedTextView) trackView.findViewById(R.id.track_page_user);
        holder.artwork = (PlayerArtworkImageView) trackView.findViewById(R.id.track_page_artwork);
        holder.waveform = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.more = trackView.findViewById(R.id.track_page_more);
        holder.close = trackView.findViewById(R.id.player_close);
        holder.bottomClose = trackView.findViewById(R.id.player_bottom_close);
        holder.nextTouch = trackView.findViewById(R.id.track_page_next);
        holder.nextButton = trackView.findViewById(R.id.player_next);
        holder.previousTouch = trackView.findViewById(R.id.track_page_previous);
        holder.previousButton = trackView.findViewById(R.id.player_previous);
        holder.playButton = trackView.findViewById(R.id.player_play);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);
        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        // Full screen player
        JaggedTextView title;
        JaggedTextView user;
        PlayerArtworkImageView artwork;
        WaveformView waveform;
        ToggleButton likeToggle;
        View more;
        View close;
        View bottomClose;
        View nextTouch;
        View nextButton;
        View previousTouch;
        View previousButton;
        View playButton;
        // Footer player
        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;

        public View[] getOnClickViews() {
            return new View[] { artwork, close, bottomClose, nextTouch, previousTouch, playButton, footer, footerPlayToggle };
        }
    }

}

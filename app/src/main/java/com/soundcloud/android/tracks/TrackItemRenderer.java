package com.soundcloud.android.tracks;

import com.google.common.base.Optional;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.presentation.CellRenderer;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    private final ImageOperations imageOperations;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;

    protected final TrackItemMenuPresenter trackItemMenuPresenter;

    private Urn playingTrack = Urn.NOT_SET;

    @Inject
    public TrackItemRenderer(ImageOperations imageOperations, TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus, ScreenProvider screenProvider, Navigator navigator) {
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.track_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        final TrackItem track = trackItems.get(position);
        getTextView(itemView, R.id.list_item_header).setText(track.getCreatorName());
        getTextView(itemView, R.id.list_item_subheader).setText(track.getTitle());
        final String formattedDuration = ScTextUtils.formatTimestamp(track.getDuration(), TimeUnit.MILLISECONDS);
        getTextView(itemView, R.id.list_item_right_info).setText(formattedDuration);

        showRelevantAdditionalInformation(itemView, track);
        toggleReposterView(itemView, track);

        loadArtwork(itemView, track);
        setupOverFlow(itemView.findViewById(R.id.overflow_button), track, position);
    }

    protected void setupOverFlow(final View button, final TrackItem track, final int position) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTrackItemMenu(button, track, position);
            }
        });
    }

    protected void showTrackItemMenu(View button, TrackItem track, int position) {
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position);
    }

    private void loadArtwork(View itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track.getEntityUrn(), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void toggleReposterView(View itemView, TrackItem track) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        final Optional<String> optionalReposter = track.getReposter();
        if (optionalReposter.isPresent()) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(optionalReposter.get());
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void showRelevantAdditionalInformation(View itemView, TrackItem track) {
        hideAllAdditionalInformation(itemView);
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track.getEntityUrn().equals(playingTrack)) {
            showNowPlaying(itemView);
        } else if (track.isPrivate()) {
            showPrivateIndicator(itemView);
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void hideAllAdditionalInformation(View itemView) {
        getTextView(itemView, R.id.list_item_counter).setVisibility(View.INVISIBLE);
        getTextView(itemView, R.id.now_playing).setVisibility(View.INVISIBLE);
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);

        TextView promoted = getTextView(itemView, R.id.promoted_track);
        promoted.setVisibility(View.GONE);
        promoted.setClickable(false);
    }

    private void showPromoted(View itemView, final PromotedTrackItem track) {
        TextView promoted = getTextView(itemView, R.id.promoted_track);
        promoted.setVisibility(View.VISIBLE);
        if (track.hasPromoter()) {
            final Context context = itemView.getContext();
            promoted.setText(context.getString(R.string.promoted_by_label, track.getPromoterName().get()));
            promoted.setClickable(true);
            promoted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigator.openProfile(context, track.getPromoterUrn().get());
                    eventBus.publish(EventQueue.TRACKING,
                            PromotedTrackEvent.forPromoterClick(track, screenProvider.getLastScreenTag()));
                }
            });
            ViewUtils.extendTouchArea(promoted, 10);
        } else {
            promoted.setText(R.string.promoted_label);
            ViewUtils.clearTouchDelegate(promoted);
        }
    }

    private void showNowPlaying(View itemView) {
        getTextView(itemView, R.id.now_playing).setVisibility(View.VISIBLE);
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void showPlayCount(View itemView, TrackItem track) {
        final int playCount = track.getPlayCount();
        if (hasPlayCount(playCount)) {
            final TextView playCountText = getTextView(itemView, R.id.list_item_counter);
            playCountText.setVisibility(View.VISIBLE);
            playCountText.setText(ScTextUtils.formatNumberWithCommas(playCount));
        }
    }

    private boolean hasPlayCount(int playCount) {
        return playCount > 0;
    }

    public void setPlayingTrack(@NotNull Urn playingTrack) {
        this.playingTrack = playingTrack;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

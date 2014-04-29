package com.soundcloud.android.collections.views;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.activities.TrackRepostActivity;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.behavior.Repost;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Transformation;
import android.widget.TextView;

/**
 * A playable row to be used in list views.
 */
public class PlayableRow extends IconLayout implements ListRow {

    protected PlayableHolder playableHolder;
    protected final TextView title;
    protected final TextView reposter;
    protected final TextView trackCount;
    private final PlaybackStateProvider playbackStateProvider;

    // used to build the string for the title text
    private SpannableStringBuilder spanBuilder;
    private final ForegroundColorSpan fcs = new ForegroundColorSpan(getResources().getColor(R.color.sc_orange));
    private final PlayablePresenter playablePresenter;

    public PlayableRow(Context context, ImageOperations imageOperations) {
        this(context, null, imageOperations);
    }

    public PlayableRow(Context context, AttributeSet attributeSet, ImageOperations imageOperations) {
        super(context, attributeSet, imageOperations);

        playablePresenter = new PlayablePresenter(context)
                .setUsernameView((TextView) findViewById(R.id.playable_user))
                .setCreatedAtView((TextView) findViewById(R.id.playable_created_at))
                .setStatsView((StatsView) findViewById(R.id.stats), true)
                .setPrivacyIndicatorView((TextView) findViewById(R.id.playable_private_indicator));

        title = (TextView) findViewById(R.id.playable_title);
        reposter = (TextView) findViewById(R.id.playable_reposter);
        trackCount = (TextView) findViewById(R.id.playable_track_count);
        playbackStateProvider = new PlaybackStateProvider();
    }

    /**
     *  update the displayed track
     * @param p the playable to display
     */
    public void setTrack(@NotNull PlayableHolder p) {
        playableHolder = p;
        playablePresenter.setPlayable(playableHolder.getPlayable());
        loadIcon();
        setTitle();
    }

    @Override
    public CharSequence getContentDescription() {
        Playable playable = playableHolder.getPlayable();

        StringBuilder builder = new StringBuilder();
        builder.append(playable.getUser().getDisplayName());
        builder.append(": ");
        builder.append(playable.title);
        builder.append(", ");

        if (playableHolder instanceof TrackRepostActivity) {
            TrackRepostActivity repost = (TrackRepostActivity) playableHolder;

            builder.append(getContext().getResources().getString(R.string.accessibility_infix_reposted_by));
            builder.append(" ");
            builder.append(repost.getUser().getDisplayName());

            builder.append(", ");
            builder.append(getTimeElapsed(getContext().getResources(), repost.getCreatedAt().getTime(), true));
            builder.append(", ");
        } else {
            builder.append(getTimeElapsed(getContext().getResources(), playable.created_at.getTime(), true));
            builder.append(", ");
        }


        // TODO: get rid of the instanceof stuff and have a track specific subclass
        int playCount = 0;
        if (playable instanceof Track && (playCount = (int) ((Track) playable).playback_count) > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_plays,
                    playCount,
                    playCount));
            builder.append(", ");
        }

        if (playable.likes_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_likes,
                    ((int) playable.likes_count),
                    ((int) playable.likes_count))
            );
            builder.append(", ");
        }

        if (playable.user_like) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_liked));
            builder.append(", ");
        }

        if (playable.reposts_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_reposts,
                    ((int) playable.reposts_count),
                    ((int) playable.reposts_count)));
            builder.append(", ");
        }

        if (playable.user_repost) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_reposted));
            builder.append(", ");
        }

        // TODO: get rid of the instanceof stuff and have a track specific subclass
        int commentCount = 0;
        if (playable instanceof Track && (commentCount = (int) ((Track) playable).comment_count) > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_comments,
                    commentCount,
                    commentCount));
        }

        return builder.toString();
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.sModelManager.getCachedTrackFromCursor(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        checkArgument(p instanceof PlayableHolder, "Not a valid playable holder: " + p);

        setTrack((PlayableHolder) p);

        Playable playable = playableHolder.getPlayable();
        setupReposter();
        setupProcessingIndicator(playable);

        // makes the row slightly transparent if not playable
        setStaticTransformationsEnabled(!playable.isStreamable());

        if (playable instanceof Playlist && ((Playlist) playable).getTrackCount() >= 0){
            trackCount.setText(String.valueOf(((Playlist) playable).getTrackCount()));
            trackCount.setVisibility(VISIBLE);
        } else {
            trackCount.setVisibility(GONE);
        }
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return inflate(getContext(), R.layout.playable_bar, this);
    }

    private void setupProcessingIndicator(Playable playable) {
        if (playable instanceof Track && ((Track) playable).isProcessing()) {
            if (findViewById(R.id.processing_progress) != null){
                findViewById(R.id.processing_progress).setVisibility(VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(GONE);
        }
    }

    private void setupReposter() {
        reposter.setVisibility(GONE);

        if (playableHolder instanceof Repost) {
            reposter.setText(((Repost) playableHolder).getReposter().username);
            reposter.setVisibility(VISIBLE);

        } else if (playableHolder instanceof SoundAssociation) {
            SoundAssociation sa = (SoundAssociation) playableHolder;
            if (sa.associationType == CollectionStorage.CollectionItemTypes.REPOST) {
                reposter.setVisibility(VISIBLE);
                User reposter = null;

                if (sa.owner == null)  {
                    // currently active user
                    if (getContext() instanceof ProfileActivity) {
                        reposter = ((ProfileActivity)getContext()).getUser();
                    }
                }
                if (reposter !=  null && reposter.getId() != SoundCloudApplication.getUserId()) {
                    this.reposter.setText(reposter.username);
                }
            }
        }
    }

    protected void setTitle() {
        if (playableHolder.getPlayable().getId() == playbackStateProvider.getCurrentTrackId()) {
            if (spanBuilder == null) spanBuilder = new SpannableStringBuilder();

            spanBuilder.clear();
            spanBuilder.append("  ");
            spanBuilder.setSpan(new ImageSpan(getContext(), R.drawable.ic_list_playing_orange, ImageSpan.ALIGN_BOTTOM),
                    0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanBuilder.append(playableHolder.getPlayable().title);
            // offset by 2 because of the speaker image and space
            spanBuilder.setSpan(fcs, 2, playableHolder.getPlayable().title.length() + 2,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            title.setText(spanBuilder);

        } else {
            title.setText(playableHolder.getPlayable().getTitle());
        }
    }

    @Override
    public Urn getResourceUrn() {
        return playableHolder.getPlayable() == null ? null : playableHolder.getPlayable().getUrn();
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
         super.getChildStaticTransformation(child, t);
         t.setAlpha(0.4f);
         return true;
     }
}

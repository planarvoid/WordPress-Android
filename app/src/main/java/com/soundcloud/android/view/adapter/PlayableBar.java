package com.soundcloud.android.view.adapter;

import static com.soundcloud.android.utils.AndroidUtils.setTextShadowForGrayBg;
import static com.soundcloud.android.utils.ScTextUtils.getTimeElapsed;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.TrackRepostActivity;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * An icon layout specialized for {@link Playable}s (tracks / playlists). It takes care of displaying playable specific
 * information such as user, title, and a timestamp. Used on the playlist and track screens, and in further
 * specialized form as list items (@see {@link PlayableRow}.
 */
public class PlayableBar extends IconLayout {
    protected PlayableHolder mPlayableHolder;
    protected TextView mTitle;
    protected TextView mUser;
    protected TextView mCreatedAt;
    // these two views don't exist for playlists
    protected @Nullable StatsView mStatsView;
    protected @Nullable TextView mPrivateIndicator;

    public PlayableBar(Context context) {
        this(context, null);
    }

    public PlayableBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mTitle = (TextView) findViewById(R.id.playable_title);
        mUser = (TextView) findViewById(R.id.playable_user);
        mCreatedAt = (TextView) findViewById(R.id.playable_created_at);
        mStatsView = (StatsView) findViewById(R.id.stats);
        mPrivateIndicator = (TextView) findViewById(R.id.playable_private_indicator);

        setViewId();
    }

    protected void setViewId() {
        setId(R.id.playable_bar);
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        TypedArray attrs = getContext().obtainStyledAttributes(attributeSet, R.styleable.PlayableBar, 0, 0);
        final int layoutId = attrs.getResourceId(R.styleable.PlayableBar_android_layout, R.layout.playable_bar);
        attrs.recycle();

        return View.inflate(getContext(), layoutId, this);
    }

    /**
     *  update the displayed track
     * @param p the playable to display
     */
    public void display(@NotNull PlayableHolder p) {

        mPlayableHolder = p;

        final Playable playable = mPlayableHolder.getPlayable();
        final Context context = getContext();

        loadIcon();

        setTitle();
        mUser.setText(playable.getUsername());
        mCreatedAt.setText(p.getTimeSinceCreated(context));

        if (mStatsView != null) {
            mStatsView.updateWithPlayable(playable, isListViewRow());
        }

        if (mPrivateIndicator != null) {
            setupPrivateIndicator(playable);
        }
    }

    private void setupPrivateIndicator(Playable playable) {
        if (playable.isPrivate()) {
            if (playable.shared_to_count <= 0) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                mPrivateIndicator.setText(R.string.tracklist_item_shared_count_unavailable);
            } else if (playable.shared_to_count == 1){
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                mPrivateIndicator.setText(playable.user_id == SoundCloudApplication.getUserId() ? R.string.tracklist_item_shared_with_1_person : R.string.tracklist_item_shared_with_you);
            } else {
                if (playable.shared_to_count < 8){
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange_states);
                } else {
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_gray_states);
                }
                mPrivateIndicator.setText(getContext().getString(R.string.tracklist_item_shared_with_x_people, playable.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        } else {
            mPrivateIndicator.setVisibility(View.GONE);
        }
    }

    protected boolean isListViewRow() {
        return false;
    }

    protected void setTitle() {
        mTitle.setText(mPlayableHolder.getPlayable().title);
    }

    public void onConnected() {
        loadIcon(); // over optimization??
    }

    public void addTextShadows() {
        setTextShadowForGrayBg(mTitle);
        setTextShadowForGrayBg(mUser);
        setTextShadowForGrayBg(mCreatedAt);
    }

    @Override
    public String getIconRemoteUri() {
        return mPlayableHolder.getPlayable().getListArtworkUrl(getContext());
    }

    @Override
    protected int getDefaultArtworkResId() {
        return R.drawable.artwork_badge;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // if not in a list, we dont want pressed state to go to textfields. this is the only way I have found
        if (isListViewRow()){
            super.dispatchSetPressed(pressed);
        }
    }

    @Override
    public CharSequence getContentDescription() {
        Playable playable = mPlayableHolder.getPlayable();

        StringBuilder builder = new StringBuilder();
        builder.append(playable.getUser().getDisplayName());
        builder.append(": ");
        builder.append(playable.title);
        builder.append(", ");

        if (mPlayableHolder instanceof TrackRepostActivity) {
            TrackRepostActivity repost = (TrackRepostActivity) mPlayableHolder;

            builder.append(getContext().getResources().getString(R.string.accessibility_infix_reposted_by));
            builder.append(" ");
            builder.append(repost.getUser().getDisplayName());

            builder.append(", ");
            builder.append(getTimeElapsed(getContext().getResources(), repost.created_at.getTime(), true));
            builder.append(", ");
        } else {
            builder.append(getTimeElapsed(getContext().getResources(), playable.created_at.getTime(), true));
            builder.append(", ");
        }


        // TODO: get rid of the instanceof stuff and have a track specific subclass
        int playCount = 0;
        if (playable instanceof Track && (playCount = ((Track) playable).playback_count) > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_plays,
                    playCount,
                    playCount));
            builder.append(", ");
        }

        if (playable.likes_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_likes,
                    playable.likes_count,
                    playable.likes_count));
            builder.append(", ");
        }

        if (playable.user_like) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_liked));
            builder.append(", ");
        }

        if (playable.reposts_count > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_reposts,
                    playable.reposts_count,
                    playable.reposts_count));
            builder.append(", ");
        }

        if (playable.user_repost) {
            builder.append(getContext().getResources().getString(R.string.accessibility_stats_user_reposted));
            builder.append(", ");
        }

        // TODO: get rid of the instanceof stuff and have a track specific subclass
        int commentCount = 0;
        if (playable instanceof Track && (commentCount = ((Track) playable).comment_count) > 0) {
            builder.append(getContext().getResources().getQuantityString(R.plurals.accessibility_stats_comments,
                    commentCount,
                    commentCount));
        }

        return builder.toString();
    }
}

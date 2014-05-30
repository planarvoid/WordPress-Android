package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.annotation.Nullable;

public class PlayablePresenter {

    private final Context mContext;
    @Nullable
    private TextView mTitleView;
    @Nullable
    private TextView mUsernameView;
    @Nullable
    private ImageView mArtworkView;
    @Nullable
    private ImageView mAvatarView;
    @Nullable
    private StatsView mStatsView;
    @Nullable
    private TextView mCreatedAtView;
    @Nullable
    private TextView mPrivateIndicator;

    private boolean mShowFullStats;

    private ApiImageSize mArtworkSize = ApiImageSize.Unknown;
    private ApiImageSize mAvatarSize = ApiImageSize.Unknown;

    private Playable mPlayable;
    private ImageOperations mImageOperations;
    private final long currentUserId;

    @Deprecated // use injection
    public PlayablePresenter(Context context) {
        mContext = context;
        mImageOperations = SoundCloudApplication.fromContext(context).getImageOperations();
        currentUserId = SoundCloudApplication.fromContext(context).getAccountOperations().getLoggedInUserId();
    }

    public PlayablePresenter setPlayableRowView(View view){
        setTitleView((TextView) view.findViewById(R.id.playable_title));
        setUsernameView((TextView) view.findViewById(R.id.playable_user));
        setStatsView((StatsView) view.findViewById(R.id.stats), true);
        setPrivacyIndicatorView((TextView) view.findViewById(R.id.playable_private_indicator));
        setCreatedAtView((TextView) view.findViewById(R.id.playable_created_at));
        return this;
    }

    public PlayablePresenter setTitleView(TextView titleView) {
        mTitleView = titleView;
        return this;
    }

    public PlayablePresenter setUsernameView(TextView usernameView) {
        mUsernameView = usernameView;
        return this;
    }

    public PlayablePresenter setTextVisibility(int visibility) {
        mTitleView.setVisibility(visibility);
        mUsernameView.setVisibility(visibility);
        return this;
    }

    public PlayablePresenter setArtwork(ImageView artworkView, ApiImageSize artworkSize) {
        mArtworkView = artworkView;
        mArtworkSize = artworkSize;
        return this;
    }

    public PlayablePresenter setAvatarView(ImageView avatarView, ApiImageSize avatarSize) {
        mAvatarView = avatarView;
        mAvatarSize = avatarSize;
        return this;
    }

    public PlayablePresenter setStatsView(StatsView statsView, boolean showFullStats) {
        mStatsView = statsView;
        mShowFullStats = showFullStats;
        return this;
    }

    public PlayablePresenter setPrivacyIndicatorView(TextView privacyIndicator) {
        mPrivateIndicator = privacyIndicator;
        return this;
    }

    public PlayablePresenter setCreatedAtView(TextView createdAtView) {
        mCreatedAtView = createdAtView;
        return this;
    }

    public void setPlayable(@NotNull Playable playable) {
        Log.d("SoundAssociations", "playable changed! " + playable.getId());
        mPlayable = playable;

        if (mTitleView != null) {
            mTitleView.setText(mPlayable.getTitle());
        }

        if (mUsernameView != null) {
            mUsernameView.setText(mPlayable.getUsername());
        }

        if (mArtworkView != null) {
            mImageOperations.displayWithPlaceholder(
                    playable.getUrn(), mArtworkSize, mArtworkView);
        }

        if (mAvatarView != null && playable.getUser() != null) {
            mImageOperations.displayWithPlaceholder(
                    playable.getUser().getUrn(), mAvatarSize, mAvatarView);
        }

        if (mStatsView != null) {
            mStatsView.updateWithPlayable(playable, mShowFullStats);
        }

        if (mCreatedAtView != null) {
            mCreatedAtView.setText(playable.getTimeSinceCreated(mContext));
        }

        if (mPrivateIndicator != null) {
            setupPrivateIndicator(playable);
        }
    }

    public Playable getPlayable() {
        return mPlayable;
    }

    public void addTextShadowForGrayBg() {
        if (mTitleView != null) AndroidUtils.setTextShadowForGrayBg(mTitleView);
        if (mUsernameView != null) AndroidUtils.setTextShadowForGrayBg(mUsernameView);
        if (mCreatedAtView != null) AndroidUtils.setTextShadowForGrayBg(mCreatedAtView);
    }

    private void setupPrivateIndicator(Playable playable) {
        if (mPrivateIndicator == null) return;

        if (playable.isPrivate()) {
            if (playable.shared_to_count <= 0) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
                mPrivateIndicator.setText(R.string.tracklist_item_shared_count_unavailable);
            } else if (playable.shared_to_count == 1) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
                mPrivateIndicator.setText(playable.user_id == currentUserId ? R.string.tracklist_item_shared_with_1_person : R.string.tracklist_item_shared_with_you);
            } else {
                if (playable.shared_to_count < 8) {
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
                } else {
                    mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_gray);
                }
                mPrivateIndicator.setText(mContext.getString(R.string.tracklist_item_shared_with_x_people, playable.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        } else {
            mPrivateIndicator.setVisibility(View.GONE);
        }
    }

}

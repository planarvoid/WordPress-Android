package com.soundcloud.android.playback.views;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.annotation.Nullable;

public class PlayableController {

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
    @Nullable
    private ToggleButton mToggleLike;
    @Nullable
    private ToggleButton mToggleRepost;
    @Nullable
    private ImageButton mShareButton;

    private boolean mShowFullStats;

    private ImageSize mArtworkSize = ImageSize.Unknown;
    private ImageSize mAvatarSize = ImageSize.Unknown;

    private int mArtworkPlaceholderResId;
    private int mAvatarPlaceholderResId;

    private final SoundAssociationOperations mSoundAssociationOps;
    private final EventBus2 mEventBus;

    private Playable mPlayable;
    private OriginProvider mOriginProvider;

    private CompositeSubscription mSubscription = new CompositeSubscription();
    private ImageOperations mImageOperations;

    public PlayableController(Context context,
                              EventBus2 eventBus,
                              SoundAssociationOperations soundAssocOperations,
                              @Nullable OriginProvider originProvider) {
        mContext = context;
        mEventBus = eventBus;
        mSoundAssociationOps = soundAssocOperations;
        mImageOperations = ImageOperations.newInstance();
        mOriginProvider = fromNullableProvider(originProvider);
    }

    private OriginProvider fromNullableProvider(@Nullable OriginProvider originProvider) {
        if (originProvider != null) {
            return originProvider;
        } else {
            return new OriginProvider() {
                @Override
                public String getScreenTag() {
                    return Screen.UNKNOWN.get();
                }
            };
        }
    }

    public void startListeningForChanges() {
        // make sure we pick up changes to the current playable that come via the event bus
        mSubscription.add(mEventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new Action1<PlayableChangedEvent>() {
            @Override
            public void call(PlayableChangedEvent event) {
                if (mPlayable != null && mPlayable.getId() == event.getPlayable().getId()) {
                    updateLikeButton((int) event.getPlayable().likes_count, event.getPlayable().user_like);
                    updateRepostButton((int) event.getPlayable().reposts_count, event.getPlayable().user_repost);
                }
            }
        }));
    }

    public void stopListeningForChanges() {
        mSubscription.unsubscribe();
    }

    public PlayableController setTitleView(TextView titleView) {
        mTitleView = titleView;
        return this;
    }

    public PlayableController setUsernameView(TextView usernameView) {
        mUsernameView = usernameView;
        return this;
    }

    public PlayableController setArtwork(ImageView artworkView, ImageSize artworkSize, int placeholderResId) {
        mArtworkView = artworkView;
        mArtworkSize = artworkSize;
        mArtworkPlaceholderResId = placeholderResId;
        return this;
    }

    public PlayableController setAvatarView(ImageView avatarView, ImageSize avatarSize, int placeholderResId) {
        mAvatarView = avatarView;
        mAvatarSize = avatarSize;
        mAvatarPlaceholderResId = placeholderResId;
        return this;
    }

    public PlayableController setStatsView(StatsView statsView, boolean showFullStats) {
        mStatsView = statsView;
        mShowFullStats = showFullStats;
        return this;
    }

    public PlayableController setPrivacyIndicatorView(TextView privacyIndicator) {
        mPrivateIndicator = privacyIndicator;
        return this;
    }

    public PlayableController setCreatedAtView(TextView createdAtView) {
        mCreatedAtView = createdAtView;
        return this;
    }

    public PlayableController setLikeButton(ToggleButton likeButton) {
        mToggleLike = likeButton;
        mToggleLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayable != null) {
                    EventBus.UI.publish(UIEvent.fromToggleLike(mToggleLike.isChecked(),
                            mOriginProvider.getScreenTag(), mPlayable));

                    mToggleLike.setEnabled(false);
                    mSubscription.add(
                            mSoundAssociationOps.toggleLike(mToggleLike.isChecked(), mPlayable)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new ResetToggleButton(mToggleLike))
                    );
                }
            }
        });
        return this;
    }

    public PlayableController setRepostButton(ToggleButton repostButton) {
        mToggleRepost = repostButton;
        mToggleRepost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayable != null) {
                    EventBus.UI.publish(UIEvent.fromToggleRepost(mToggleRepost.isChecked(),
                            mOriginProvider.getScreenTag(), mPlayable));

                    mToggleRepost.setEnabled(false);
                    mSubscription.add(
                            mSoundAssociationOps.toggleRepost(mToggleRepost.isChecked(), mPlayable)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new ResetToggleButton(mToggleRepost))
                    );
                }
            }
        });
        return this;
    }

    public PlayableController setShareButton(@NotNull ImageButton shareButton) {
        mShareButton = shareButton;
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayable != null) {
                    EventBus.UI.publish(UIEvent.fromShare(mOriginProvider.getScreenTag(), mPlayable));
                    Intent shareIntent = mPlayable.getShareIntent();
                    if (shareIntent != null) {
                        mContext.startActivity(shareIntent);
                    }
                }
            }
        });
        return this;
    }

    public PlayableController setAddToPlaylistButton(View addToPlaylistButton, final AddToPlaylistListener listener) {
        addToPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayable instanceof Track && listener != null) {
                    listener.onAddToPlaylist((Track) mPlayable);
                }
            }
        });
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
            mImageOperations.displayPlaceholder(mArtworkSize.formatUri(mPlayable.getArtwork()), mArtworkView, mArtworkPlaceholderResId);
        }

        if (mAvatarView != null) {
            mImageOperations.displayPlaceholder(mAvatarSize.formatUri(mPlayable.getAvatarUrl()), mAvatarView, mAvatarPlaceholderResId);
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

        if (mToggleLike != null) {
            updateLikeButton((int) mPlayable.likes_count, mPlayable.user_like);
        }

        if (mToggleRepost != null) {
            updateRepostButton((int) mPlayable.reposts_count, mPlayable.user_repost);
        }

        boolean showRepost = mPlayable.isPublic() && mPlayable.getUserId() != SoundCloudApplication.getUserId();
        if (mToggleRepost != null) {
            mToggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        }

        if (mShareButton != null) {
            mShareButton.setVisibility(mPlayable.isPublic() ? View.VISIBLE : View.GONE);
        }
    }

    public void setOriginProvider(OriginProvider originProvider) {
        mOriginProvider = originProvider;
    }


    private void updateLikeButton(int count, boolean userLiked) {
        updateToggleButton(mToggleLike,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    private void updateRepostButton(int count, boolean userReposted) {
        updateToggleButton(mToggleRepost,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                count,
                userReposted,
                R.string.accessibility_stats_user_reposted);
    }

    private void updateToggleButton(@Nullable ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked,
                                    int checkedStringId) {
        if (button == null) return;
        Log.d(SoundAssociationOperations.TAG, Thread.currentThread().getName() + ": update button state: count = " + count + "; checked = " + checked);
        button.setEnabled(true);
        final String buttonLabel = labelForCount(count);
        button.setTextOn(buttonLabel);
        button.setTextOff(buttonLabel);
        button.setChecked(checked);
        button.invalidate();


        if (AndroidUtils.accessibilityFeaturesAvailable(mContext)
                && TextUtils.isEmpty(button.getContentDescription())) {
            final StringBuilder builder = new StringBuilder();
            builder.append(mContext.getResources().getString(actionStringID));

            if (count >= 0) {
                builder.append(", ");
                builder.append(mContext.getResources().getQuantityString(descriptionPluralID, count, count));
            }

            if (checked) {
                builder.append(", ");
                builder.append(mContext.getResources().getString(checkedStringId));
            }

            button.setContentDescription(builder.toString());
        }
    }

    @VisibleForTesting
    String labelForCount(int count) {
        if (count <= 0) {
            return "";
        } else if (count >= 10000) {
            return "9k+"; // top out at 9k or text gets too long again
        } else if (count >= 1000) {
            return count / 1000 + "k+";
        } else {
            return String.valueOf(count);
        }
    }

    private void setupPrivateIndicator(Playable playable) {
        if (mPrivateIndicator == null) return;

        if (playable.isPrivate()) {
            if (playable.shared_to_count <= 0) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
                mPrivateIndicator.setText(R.string.tracklist_item_shared_count_unavailable);
            } else if (playable.shared_to_count == 1) {
                mPrivateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
                mPrivateIndicator.setText(playable.user_id == SoundCloudApplication.getUserId() ? R.string.tracklist_item_shared_with_1_person : R.string.tracklist_item_shared_with_you);
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

    private static final class ResetToggleButton extends DefaultObserver<SoundAssociation> {
        private final ToggleButton mToggleButton;

        private ResetToggleButton(ToggleButton toggleButton) {
            mToggleButton = toggleButton;
        }

        @Override
        public void onError(Throwable e) {
            mToggleButton.setChecked(!mToggleButton.isChecked());
            mToggleButton.setEnabled(true);
            super.onError(e);
        }
    }

    public interface AddToPlaylistListener {
        void onAddToPlaylist(Track track);
    }
}

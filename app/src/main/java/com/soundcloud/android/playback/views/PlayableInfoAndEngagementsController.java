package com.soundcloud.android.playback.views;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.collections.views.PlayableBar;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.annotation.Nullable;

public class PlayableInfoAndEngagementsController {

    private View mRootView;
    @Nullable
    private ToggleButton mToggleLike;
    @Nullable
    private ToggleButton mToggleRepost;
    @Nullable
    private ImageButton mShareButton;
    @Nullable
    private PlayableBar mTrackInfoBar;

    private Playable mPlayable;
    private OriginProvider mOriginProvider;

    private CompositeSubscription mSubscription = new CompositeSubscription();

    public PlayableInfoAndEngagementsController(View rootView,
                                                final @Nullable PlayerTrackView.PlayerTrackViewListener mListener,
                                                final SoundAssociationOperations soundAssocOperations,
                                                @Nullable OriginProvider originProvider) {

        mRootView = rootView;
        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);

       if (originProvider == null) {
           setUnknownOrigin();
       } else {
           mOriginProvider = originProvider;
       }

        if (mToggleLike != null) {
            mToggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        EventBus.UI.publish(UIEvent.fromToggleLike(mToggleLike.isChecked(),
                                mOriginProvider.getScreenTag(), mPlayable));

                        mToggleLike.setEnabled(false);
                        mSubscription.add(
                                soundAssocOperations.toggleLike(mToggleLike.isChecked(), mPlayable)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new ResetToggleButton(mToggleLike))
                        );
                    }
                }
            });
        }

        if (mToggleRepost != null) {
            mToggleRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        EventBus.UI.publish(UIEvent.fromToggleRepost(mToggleRepost.isChecked(),
                            mOriginProvider.getScreenTag(), mPlayable));

                        mToggleRepost.setEnabled(false);
                        mSubscription.add(
                                soundAssocOperations.toggleRepost(mToggleRepost.isChecked(), mPlayable)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new ResetToggleButton(mToggleRepost))
                        );
                    }
                }
            });
        }

        if (mShareButton != null) {
            mShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable != null) {
                        EventBus.UI.publish(UIEvent.fromShare(mOriginProvider.getScreenTag(), mPlayable));
                        Intent shareIntent = mPlayable.getShareIntent();
                        if (shareIntent != null) {
                            mRootView.getContext().startActivity(shareIntent);
                        }
                    }
                }
            });
        }

        if (rootView.findViewById(R.id.btn_addToSet) != null) {
            rootView.findViewById(R.id.btn_addToSet).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable instanceof Track && mListener != null) {
                        mListener.onAddToPlaylist((Track) mPlayable);
                    }
                }
            });
        }

        mTrackInfoBar = (PlayableBar) rootView.findViewById(R.id.playable_bar);
        if (mTrackInfoBar != null) {
            if (mTrackInfoBar.findViewById(R.id.playable_private_indicator) != null){
                mTrackInfoBar.findViewById(R.id.playable_private_indicator).setVisibility(View.GONE);
            }

            mTrackInfoBar.addTextShadows();
            mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(mTrackInfoBar.getContext(), mPlayable);
                }
            });
        }

        // make sure we pick up changes to the current playable that come via the event bus
        mSubscription.add(EventBus.PLAYABLE_CHANGED.subscribe(new Action1<PlayableChangedEvent>() {
            @Override
            public void call(PlayableChangedEvent event) {
                if (mPlayable != null && mPlayable.getId() == event.getPlayable().getId()) {
                    setLikes((int) event.getPlayable().likes_count, event.getPlayable().user_like);
                    setReposts((int) event.getPlayable().reposts_count, event.getPlayable().user_repost);
                }
            }
        }));
    }

    private void setUnknownOrigin() {
        mOriginProvider = new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.UNKNOWN.get();
            }
        };
    }

    public void onDestroy() {
        mSubscription.unsubscribe();
    }

    public void setTrack(@NotNull Playable playable) {
        Log.d("SoundAssociations", "playable changed! " + playable.getId());
        mPlayable = playable;

        if (mToggleLike != null) {
            setLikes((int) mPlayable.likes_count, mPlayable.user_like);
        }
        if (mToggleRepost != null) {
            setReposts((int) mPlayable.reposts_count, mPlayable.user_repost);
        }

        boolean showRepost = mPlayable.isPublic() && mPlayable.getUserId() != SoundCloudApplication.getUserId();
        if (mToggleRepost != null) {
            mToggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        }
        if (mShareButton != null) {
            mShareButton.setVisibility(mPlayable.isPublic() ? View.VISIBLE : View.GONE);
        }
        if (mTrackInfoBar != null){
            mTrackInfoBar.setTrack(playable);
        }
    }

    public void setOriginProvider(OriginProvider originProvider) {
        mOriginProvider = originProvider;
    }

    public void update(ToggleButton button, int actionStringID, int descriptionPluralID, int count, boolean checked,
                       int checkedStringId) {
        Log.d(SoundAssociationOperations.TAG, Thread.currentThread().getName() +  ": update button state: count = " + count + "; checked = " + checked);
        button.setEnabled(true);
        final String buttonLabel = labelForCount(count);
        button.setTextOn(buttonLabel);
        button.setTextOff(buttonLabel);
        button.setChecked(checked);
        button.invalidate();

        Context context = mRootView.getContext();
        if (AndroidUtils.accessibilityFeaturesAvailable(context)
                && TextUtils.isEmpty(button.getContentDescription())) {
            final StringBuilder builder = new StringBuilder();
            builder.append(context.getResources().getString(actionStringID));

            if (count >= 0) {
                builder.append(", ");
                builder.append(context.getResources().getQuantityString(descriptionPluralID, count, count));
            }

            if (checked) {
                builder.append(", ");
                builder.append(context.getResources().getString(checkedStringId));
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

    private void setLikes(int count, boolean userLiked) {
        update(mToggleLike,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    private void setReposts(int count, boolean userReposted) {
        update(mToggleRepost,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                count,
                userReposted,
                R.string.accessibility_stats_user_reposted);
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
}

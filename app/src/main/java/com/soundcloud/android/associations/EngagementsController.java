package com.soundcloud.android.associations;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class EngagementsController {

    private Context mContext;

    @Nullable
    private ToggleButton mToggleLike;
    @Nullable
    private ToggleButton mToggleRepost;
    @Nullable
    private ImageButton mShareButton;
    @Nullable
    private AddToPlaylistListener mAddToPlaylistListener;

    private Playable mPlayable;
    private final SoundAssociationOperations mSoundAssociationOps;
    private OriginProvider mOriginProvider;
    private final EventBus mEventBus;

    private CompositeSubscription mSubscription = new CompositeSubscription();

    public interface AddToPlaylistListener {
        void onAddToPlaylist(Track track);
    }

    @Inject
    public EngagementsController(EventBus eventBus, SoundAssociationOperations soundAssocOperations) {
        mEventBus = eventBus;
        mSoundAssociationOps = soundAssocOperations;
    }

    public void bindView(View rootView) {
        bindView(rootView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.UNKNOWN.get();
            }
        });
    }

    public void bindView(View rootView, OriginProvider originProvider) {
        mContext = rootView.getContext();
        mOriginProvider = originProvider;

        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        if (mToggleLike != null) {
            mToggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        mEventBus.publish(EventQueue.UI, UIEvent.fromToggleLike(mToggleLike.isChecked(),
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
        }

        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        if (mToggleRepost != null) {
            mToggleRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPlayable != null) {
                        mEventBus.publish(EventQueue.UI, UIEvent.fromToggleRepost(mToggleRepost.isChecked(),
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
        }

        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);
        if (mShareButton != null) {
            mShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable != null) {
                        mEventBus.publish(EventQueue.UI, UIEvent.fromShare(mOriginProvider.getScreenTag(), mPlayable));
                        Intent shareIntent = mPlayable.getShareIntent();
                        if (shareIntent != null) {
                            mContext.startActivity(shareIntent);
                        }
                    }
                }
            });
        }

        ImageButton mAddToPlaylistBtn = (ImageButton) rootView.findViewById(R.id.btn_addToPlaylist);
        if (mAddToPlaylistBtn != null) {
            mAddToPlaylistBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable instanceof Track && mAddToPlaylistListener != null) {
                        mAddToPlaylistListener.onAddToPlaylist((Track) mPlayable);
                    }
                }
            });
        }
    }

    public void startListeningForChanges() {
        // make sure we pick up changes to the current playable that come via the event bus
        mSubscription.add(mEventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new DefaultSubscriber<PlayableChangedEvent>() {
            @Override
            public void onNext(PlayableChangedEvent event) {
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

    public void setAddToPlaylistListener(AddToPlaylistListener listener) {
        mAddToPlaylistListener = listener;
    }

    public void setOriginProvider(OriginProvider originProvider) {
        mOriginProvider = originProvider;
    }

    public void setPlayable(@NotNull Playable playable) {
        Log.d("SoundAssociations", "playable changed! " + playable.getId());
        mPlayable = playable;

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

    private static final class ResetToggleButton extends DefaultSubscriber<SoundAssociation> {
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

package com.soundcloud.android.associations;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
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

public class EngagementsController {

    private final Context mContext;

    @Nullable
    private ToggleButton mToggleLike;
    @Nullable
    private ToggleButton mToggleRepost;
    @Nullable
    private ImageButton mShareButton;
    @Nullable
    private ImageButton mAddToPlaylistBtn;

    private Playable mPlayable;
    private final SoundAssociationOperations mSoundAssociationOps;
    private OriginProvider mOriginProvider;

    private CompositeSubscription mSubscription = new CompositeSubscription();

    public interface AddToPlaylistListener {
        void onAddToPlaylist(Track track);
    }

    public EngagementsController(Context context, View rootView,
                                 SoundAssociationOperations soundAssocOperations,
                                 @Nullable OriginProvider originProvider) {
        this(context, rootView, soundAssocOperations, originProvider, null);
    }

    public EngagementsController(Context context, View rootView,
                                 SoundAssociationOperations soundAssocOperations,
                                 @Nullable OriginProvider originProvider, final AddToPlaylistListener listener) {

        mContext = context;
        mSoundAssociationOps = soundAssocOperations;
        OriginProvider result;
        result = originProvider != null ? originProvider : new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.UNKNOWN.get();
            }
        };
        mOriginProvider = result;

        mToggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        if (mToggleLike != null){
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
        }

        mToggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        if (mToggleRepost != null){
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
        }

        mShareButton = (ImageButton) rootView.findViewById(R.id.btn_share);
        if (mShareButton != null){
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
        }

        mAddToPlaylistBtn = (ImageButton) rootView.findViewById(R.id.btn_addToPlaylist);
        if (mAddToPlaylistBtn != null){
            mAddToPlaylistBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayable instanceof Track && listener != null) {
                        listener.onAddToPlaylist((Track) mPlayable);
                    }
                }
            });
        }
    }

    public void startListeningForChanges() {
        // make sure we pick up changes to the current playable that come via the event bus
        mSubscription.add(EventBus.PLAYABLE_CHANGED.subscribe(new Action1<PlayableChangedEvent>() {
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

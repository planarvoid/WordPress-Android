package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
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

    private Context context;

    @Nullable
    private ToggleButton toggleLike;
    @Nullable
    private ToggleButton toggleRepost;
    @Nullable
    private ImageButton shareButton;
    @Nullable
    private AddToPlaylistListener addToPlaylistListener;

    private Playable playable;
    private OriginProvider originProvider;
    private final SoundAssociationOperations soundAssociationOps;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;

    private CompositeSubscription subscription = new CompositeSubscription();

    public interface AddToPlaylistListener {
        void onAddToPlaylist(Track track);
    }

    @Inject
    public EngagementsController(EventBus eventBus, SoundAssociationOperations soundAssociationOps,
                                 AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.soundAssociationOps = soundAssociationOps;
        this.accountOperations = accountOperations;
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
        context = rootView.getContext();
        this.originProvider = originProvider;

        toggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        if (toggleLike != null) {
            toggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (playable != null) {
                        eventBus.publish(EventQueue.UI, UIEvent.fromToggleLike(toggleLike.isChecked(),
                                EngagementsController.this.originProvider.getScreenTag(), playable));

                        toggleLike.setEnabled(false);
                        subscription.add(
                                soundAssociationOps.toggleLike(toggleLike.isChecked(), playable)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new ResetToggleButton(toggleLike))
                        );
                    }
                }
            });
        }

        toggleRepost = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        if (toggleRepost != null) {
            toggleRepost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (playable != null) {
                        eventBus.publish(EventQueue.UI, UIEvent.fromToggleRepost(toggleRepost.isChecked(),
                                EngagementsController.this.originProvider.getScreenTag(), playable));

                        toggleRepost.setEnabled(false);
                        subscription.add(
                                soundAssociationOps.toggleRepost(toggleRepost.isChecked(), playable)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new ResetToggleButton(toggleRepost))
                        );
                    }
                }
            });
        }

        shareButton = (ImageButton) rootView.findViewById(R.id.btn_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playable != null) {
                        eventBus.publish(EventQueue.UI, UIEvent.fromShare(EngagementsController.this.originProvider.getScreenTag(), playable));
                        Intent shareIntent = playable.getShareIntent();
                        if (shareIntent != null) {
                            context.startActivity(shareIntent);
                        }
                    }
                }
            });
        }

        ImageButton addToPlaylistBtn = (ImageButton) rootView.findViewById(R.id.btn_addToPlaylist);
        if (addToPlaylistBtn != null) {
            addToPlaylistBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playable instanceof Track && addToPlaylistListener != null) {
                        addToPlaylistListener.onAddToPlaylist((Track) playable);
                    }
                }
            });
        }
    }

    public void startListeningForChanges() {
        subscription = new CompositeSubscription();
        // make sure we pick up changes to the current playable that come via the event bus
        subscription.add(eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new DefaultSubscriber<PlayableChangedEvent>() {
            @Override
            public void onNext(PlayableChangedEvent event) {
                if (playable != null && playable.getUrn().equals(event.getUrn())) {
                    final PropertySet changeSet = event.getChangeSet();
                    if (changeSet.contains(PlayableProperty.IS_LIKED)) {
                        updateLikeButton(
                                changeSet.get(PlayableProperty.LIKES_COUNT),
                                changeSet.get(PlayableProperty.IS_LIKED));
                    }
                    if (changeSet.contains(PlayableProperty.IS_REPOSTED)) {
                        updateRepostButton(
                                changeSet.get(PlayableProperty.REPOSTS_COUNT),
                                changeSet.get(PlayableProperty.IS_REPOSTED));
                    }
                }
            }
        }));
    }

    public void stopListeningForChanges() {
        subscription.unsubscribe();
    }

    public void setAddToPlaylistListener(AddToPlaylistListener listener) {
        addToPlaylistListener = listener;
    }

    public void setOriginProvider(OriginProvider originProvider) {
        this.originProvider = originProvider;
    }

    public void setPlayable(@NotNull Playable playable) {
        Log.d("SoundAssociations", "playable changed! " + playable.getId());
        this.playable = playable;

        if (toggleLike != null) {
            updateLikeButton(this.playable.likes_count, this.playable.user_like);
        }

        if (toggleRepost != null) {
            updateRepostButton(this.playable.reposts_count, this.playable.user_repost);
        }

        boolean showRepost = this.playable.isPublic() && this.playable.getUserId() != accountOperations.getLoggedInUserId();
        if (toggleRepost != null) {
            toggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        }

        if (shareButton != null) {
            shareButton.setVisibility(this.playable.isPublic() ? View.VISIBLE : View.GONE);
        }
    }


    private void updateLikeButton(int count, boolean userLiked) {
        updateToggleButton(toggleLike,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    private void updateRepostButton(int count, boolean userReposted) {
        updateToggleButton(toggleRepost,
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
        final String buttonLabel = ScTextUtils.shortenLargeNumber(count);
        button.setTextOn(buttonLabel);
        button.setTextOff(buttonLabel);
        button.setChecked(checked);
        button.invalidate();


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

    private static final class ResetToggleButton extends DefaultSubscriber<SoundAssociation> {
        private final ToggleButton toggleButton;

        private ResetToggleButton(ToggleButton toggleButton) {
            this.toggleButton = toggleButton;
        }

        @Override
        public void onError(Throwable e) {
            toggleButton.setChecked(!toggleButton.isChecked());
            toggleButton.setEnabled(true);
            super.onError(e);
        }
    }
}

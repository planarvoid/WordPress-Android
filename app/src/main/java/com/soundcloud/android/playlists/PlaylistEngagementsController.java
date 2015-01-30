package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.associations.LegacyRepostOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class PlaylistEngagementsController {

    private static final String SHARE_TYPE = "text/plain";

    @Nullable private ToggleButton toggleLike;
    @Nullable private ToggleButton toggleRepost;
    @Nullable private ImageButton shareButton;

    private Context context;
    private Playable playable;
    private OriginProvider originProvider;

    private final LegacyRepostOperations soundAssociationOps;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    public PlaylistEngagementsController(EventBus eventBus, LegacyRepostOperations soundAssociationOps,
                                         AccountOperations accountOperations, LikeOperations likeOperations) {
        this.eventBus = eventBus;
        this.soundAssociationOps = soundAssociationOps;
        this.accountOperations = accountOperations;
        this.likeOperations = likeOperations;
    }

    void bindView(View rootView) {
        bindView(rootView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.UNKNOWN.get();
            }
        });
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    void bindView(View rootView, OriginProvider originProvider) {
        this.context = rootView.getContext();
        this.originProvider = originProvider;

        toggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        if (toggleLike != null) {
            toggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean addLike = toggleLike.isChecked();
                    if (playable != null) {
                        final PropertySet propertySet = playable.toPropertySet();

                        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike,
                                Screen.PLAYLIST_DETAILS.get(),
                                PlaylistEngagementsController.this.originProvider.getScreenTag(),
                                playable.getUrn()));

                        fireAndForget(addLike
                                ? likeOperations.addLike(propertySet)
                                : likeOperations.removeLike(propertySet));
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
                        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleRepost(toggleRepost.isChecked(),
                                PlaylistEngagementsController.this.originProvider.getScreenTag(), playable.getUrn()));
                        fireAndForget(soundAssociationOps.toggleRepost(playable.getUrn(), toggleRepost.isChecked()));
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
                        eventBus.publish(EventQueue.TRACKING,
                                UIEvent.fromShare(PlaylistEngagementsController.this.originProvider.getScreenTag(), playable.getUrn()));
                        sendShareIntent();
                    }
                }
            });
        }
    }

    private void sendShareIntent() {
        if (playable.isPrivate()) {
            return;
        }

        Intent shareIntent = buildShareIntent();
        if (shareIntent != null) {
            context.startActivity(shareIntent);
        }
    }

    private Intent buildShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, playable.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildText());
        return shareIntent;
    }

    private String buildText() {
        if (ScTextUtils.isNotBlank(playable.getUsername())) {
            return context.getString(R.string.share_track_by_artist_on_soundcloud, playable.getTitle(), playable.getUsername(), playable.permalink_url);
        }
        return context.getString(R.string.share_track_on_soundcloud, playable.getTitle(), playable.permalink_url);
    }

    void startListeningForChanges() {
        subscription = new CompositeSubscription();
        // make sure we pick up changes to the current playable that come via the event bus
        subscription.add(eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new DefaultSubscriber<PlayableUpdatedEvent>() {
            @Override
            public void onNext(PlayableUpdatedEvent event) {
                if (playable != null && playable.getUrn().equals(event.getUrn())) {
                    final PropertySet changeSet = event.getChangeSet();
                    playable.updateAssociations(changeSet);

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

    void stopListeningForChanges() {
        subscription.unsubscribe();
    }

    void setOriginProvider(OriginProvider originProvider) {
        this.originProvider = originProvider;
    }

    void setPlayable(@NotNull Playable playable) {
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
        if (button == null) {
            return;
        }
        Log.d(LegacyRepostOperations.TAG, Thread.currentThread().getName() + ": update button state: count = " + count + "; checked = " + checked);
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

}

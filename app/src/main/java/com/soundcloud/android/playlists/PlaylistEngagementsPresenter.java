package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.LegacyRepostOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
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
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class PlaylistEngagementsPresenter {

    private static final String SHARE_TYPE = "text/plain";

    @Nullable private ToggleButton toggleLike;
    @Nullable private ToggleButton toggleRepost;
    @Nullable private ImageButton shareButton;

    private Context context;
    private PlaylistInfo playlistInfo;
    private OriginProvider originProvider;

    private final LegacyRepostOperations soundAssociationOps;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    public PlaylistEngagementsPresenter(EventBus eventBus,
                                        LegacyRepostOperations soundAssociationOps,
                                        AccountOperations accountOperations,
                                        LikeOperations likeOperations) {
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

        final ViewGroup holder = (ViewGroup) rootView.findViewById(R.id.playlist_action_bar_holder);
        View.inflate(rootView.getContext(), R.layout.playlist_action_bar, holder);

        toggleLike = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        if (toggleLike != null) {
            toggleLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean addLike = toggleLike.isChecked();
                    if (playlistInfo != null) {
                        final PropertySet propertySet = playlistInfo.getSourceSet();

                        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike,
                                Screen.PLAYLIST_DETAILS.get(),
                                PlaylistEngagementsPresenter.this.originProvider.getScreenTag(),
                                playlistInfo.getUrn()));

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
                    if (playlistInfo != null) {
                        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleRepost(toggleRepost.isChecked(),
                                PlaylistEngagementsPresenter.this.originProvider.getScreenTag(), playlistInfo.getUrn()));
                        fireAndForget(soundAssociationOps.toggleRepost(playlistInfo.getUrn(), toggleRepost.isChecked()));
                    }
                }
            });
        }

        shareButton = (ImageButton) rootView.findViewById(R.id.btn_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (playlistInfo != null) {
                        eventBus.publish(EventQueue.TRACKING,
                                UIEvent.fromShare(PlaylistEngagementsPresenter.this.originProvider.getScreenTag(), playlistInfo.getUrn()));
                        sendShareIntent();
                    }
                }
            });
        }
    }

    private void sendShareIntent() {
        if (playlistInfo.isPrivate()) {
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
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, playlistInfo.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildText());
        return shareIntent;
    }

    private String buildText() {
        if (ScTextUtils.isNotBlank(playlistInfo.getCreatorName())) {
            return context.getString(R.string.share_track_by_artist_on_soundcloud,
                    playlistInfo.getTitle(), playlistInfo.getCreatorName(), playlistInfo.getPermalinkUrl());
        }
        return context.getString(R.string.share_track_on_soundcloud, playlistInfo.getTitle(), playlistInfo.getPermalinkUrl());
    }

    void startListeningForChanges() {
        subscription = new CompositeSubscription();
        subscription.add(eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new DefaultSubscriber<EntityStateChangedEvent>() {
            @Override
            public void onNext(EntityStateChangedEvent event) {
                if (playlistInfo != null && playlistInfo.getUrn().equals(event.getNextUrn())) {
                    final PropertySet changeSet = event.getNextChangeSet();
                    playlistInfo.update(changeSet);

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

    void setPlaylistInfo(@NotNull PlaylistInfo playlistInfo) {
        this.playlistInfo = playlistInfo;

        if (toggleLike != null) {
            updateLikeButton(this.playlistInfo.getLikesCount(), this.playlistInfo.isLikedByUser());
        }

        if (toggleRepost != null) {
            updateRepostButton(this.playlistInfo.getRepostsCount(), this.playlistInfo.isRepostedByUser());
        }

        boolean showRepost = this.playlistInfo.isPublic() && !accountOperations.isLoggedInUser(playlistInfo.getCreatorUrn());
        if (toggleRepost != null) {
            toggleRepost.setVisibility(showRepost ? View.VISIBLE : View.GONE);
        }

        if (shareButton != null) {
            shareButton.setVisibility(this.playlistInfo.isPublic() ? View.VISIBLE : View.GONE);
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

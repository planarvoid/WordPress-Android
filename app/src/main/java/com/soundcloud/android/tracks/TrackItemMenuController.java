package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.TrackMenuWrapperListener;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

public final class TrackItemMenuController implements TrackMenuWrapperListener {
    private final PlayQueueManager playQueueManager;
    private final SoundAssociationOperations associationOperations;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final TrackStorage trackStorage;
    private final AccountOperations accountOperations;
    private final Context context;

    private FragmentActivity activity;
    private PropertySet track;
    private EventBus eventBus;
    private Subscription trackSubscription = Subscriptions.empty();

    @Inject
    TrackItemMenuController(PlayQueueManager playQueueManager,
                            SoundAssociationOperations associationOperations,
                            PopupMenuWrapper.Factory popupMenuWrapperFactory,
                            TrackStorage trackStorage,
                            AccountOperations accountOperations,
                            EventBus eventBus, Context context) {
        this.playQueueManager = playQueueManager;
        this.associationOperations = associationOperations;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.trackStorage = trackStorage;
        this.accountOperations = accountOperations;
        this.eventBus = eventBus;
        this.context = context;
    }

    public void show(FragmentActivity activity, View button, PropertySet track) {
        this.activity = activity;
        this.track = track;
        final PopupMenuWrapper menu = setupMenu(button);
        loadTrack(menu);
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.track_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.setItemEnabled(R.id.add_to_likes, false);
        menu.show();
        return menu;
    }

    private void loadTrack(PopupMenuWrapper menu) {
        trackSubscription.unsubscribe();
        trackSubscription = trackStorage
                .track(track.get(TrackProperty.URN), accountOperations.getLoggedInUserUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackSubscriber(track, menu));
    }

    @Override
    public void onDismiss() {
        trackSubscription.unsubscribe();
        trackSubscription = Subscriptions.empty();
        activity = null;
        track = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                handleLike();
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog();
                return true;
            default:
                return false;
        }
    }

    private void handleLike() {
        final Urn trackUrn = track.get(TrackProperty.URN);
        final Boolean newLikeStatus = !track.get(TrackProperty.IS_LIKED);
        associationOperations
                .toggleLike(trackUrn, newLikeStatus)
                .doOnNext(new Action1<PropertySet>() {
                    @Override
                    public void call(PropertySet bindings) {
                        if (newLikeStatus != bindings.get(TrackProperty.IS_LIKED)) {
                            throw new IllegalStateException("Track did not change liked status on server side");
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(context));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(newLikeStatus, playQueueManager.getScreenTag(), trackUrn));
    }

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track, playQueueManager.getScreenTag());
        from.show(activity.getSupportFragmentManager());
    }

    private static class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        private final PropertySet track;
        private final PopupMenuWrapper menu;

        public TrackSubscriber(PropertySet track, PopupMenuWrapper menu) {
            this.track = track;
            this.menu = menu;
        }

        @Override
        public void onNext(PropertySet details) {
            track.merge(details);
            updateLikeActionTitle(track.get(TrackProperty.IS_LIKED));
            menu.setItemEnabled(R.id.add_to_likes, true);
        }

        private void updateLikeActionTitle(boolean isLiked) {
            final MenuItem item = menu.findItem(R.id.add_to_likes);
            if (isLiked) {
                item.setTitle(R.string.unlike);
            } else {
                item.setTitle(R.string.like);
            }
        }
    }

    private static class LikeToggleSubscriber extends DefaultSubscriber<PropertySet> {
        private final Context context;

        private LikeToggleSubscriber(Context context) {
            this.context = context;
        }

        @Override
        public void onNext(PropertySet likeStatus) {
            if (likeStatus.get(PlayableProperty.IS_LIKED)) {
                Toast.makeText(context, R.string.like_toast_overflow_action, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.unlike_toast_overflow_action, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable e) {
            Toast.makeText(context, R.string.like_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
        }
    }
}

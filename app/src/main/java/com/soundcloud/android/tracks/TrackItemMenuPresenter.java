package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.PopupMenuWrapperListener;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

public final class TrackItemMenuPresenter implements PopupMenuWrapperListener {
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final LoadTrackCommand loadTrackCommand;
    private final Context context;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final ScreenProvider screenProvider;

    private FragmentActivity activity;
    private PropertySet track;
    private Subscription trackSubscription = Subscriptions.empty();

    @Inject
    TrackItemMenuPresenter(PopupMenuWrapper.Factory popupMenuWrapperFactory,
                           LoadTrackCommand loadTrackCommand,
                           EventBus eventBus, Context context,
                           LikeOperations likeOperations, ScreenProvider screenProvider) {
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.loadTrackCommand = loadTrackCommand;
        this.eventBus = eventBus;
        this.context = context;
        this.likeOperations = likeOperations;
        this.screenProvider = screenProvider;
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
        trackSubscription = loadTrackCommand
                .with(track.get(TrackProperty.URN))
                .toObservable()
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

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track, ScreenElement.LIST.get(),
                screenProvider.getLastScreenTag());
        from.show(activity.getSupportFragmentManager());
    }

    private void handleLike() {
        final Urn trackUrn = track.get(TrackProperty.URN);
        final Boolean addLike = !track.get(TrackProperty.IS_LIKED);
        getToggleLikeObservable(addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(context, addLike));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike, ScreenElement.LIST.get(),
                screenProvider.getLastScreenTag(), trackUrn));
    }

    private Observable<PropertySet> getToggleLikeObservable(boolean addLike) {
        return addLike ? likeOperations.addLike(track) : likeOperations.removeLike(track);
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
            track.update(details);
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
        private final boolean likeStatus;

        LikeToggleSubscriber(Context context, boolean likeStatus) {
            this.context = context;
            this.likeStatus = likeStatus;
        }

        @Override
        public void onNext(PropertySet ignored) {
            if (likeStatus) {
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

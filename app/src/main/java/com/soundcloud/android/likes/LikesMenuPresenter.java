package com.soundcloud.android.likes;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;
import javax.inject.Provider;

class LikesMenuPresenter {

    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineOperations;
    private final Provider<OfflineLikesDialog> syncLikesDialogProvider;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;

    private boolean optionsMenuPrepared;

    @Inject
    public LikesMenuPresenter(FeatureOperations featureOperations,
                              OfflineContentOperations offlineContentOperations,
                              Provider<OfflineLikesDialog> syncLikesDialogProvider,
                              ScreenProvider screenProvider,
                              Navigator navigator,
                              EventBus eventBus) {
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineContentOperations;
        this.syncLikesDialogProvider = syncLikesDialogProvider;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.likes_actions, menu);
        optionsMenuPrepared = false;
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem menuItem, Context context, FragmentManager fragmentManager) {
        switch (menuItem.getItemId()) {
            case R.id.action_make_offline_available:
                makeOfflineAvailable(context, fragmentManager);
                return true;
            case R.id.action_make_offline_unavailable:
                makeOfflineUnavailable(fragmentManager);
                return true;
            default:
                return false;
        }
    }

    private void makeOfflineAvailable(Context context, FragmentManager fragmentManager) {
        if (featureOperations.isOfflineContentEnabled()) {
            syncLikesDialogProvider.get().show(fragmentManager);
        } else {
            navigator.openUpgrade(context);
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forLikesClick());
        }
    }

    private void makeOfflineUnavailable(FragmentManager fragmentManager) {
        if (offlineOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForLikes(fragmentManager);
        } else {
            fireAndForget(offlineOperations.disableOfflineLikedTracks());
            eventBus.publish(EventQueue.TRACKING,
                    OfflineInteractionEvent.fromRemoveOfflineLikes(screenProvider.getLastScreenTag()));
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (featureOperations.isOfflineContentOrUpsellEnabled()) {
            configureOfflineContentMenu(menu);
            if (featureOperations.upsellOfflineContent() && optionsMenuPrepared) {
                eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forLikesImpression());
            }
        }
        optionsMenuPrepared = true;
    }

    private void configureOfflineContentMenu(Menu menu) {
        offlineOperations
                .isOfflineLikedTracksEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new UpdatePopupMenuOptionsSubscriber(menu));
    }

    private final class UpdatePopupMenuOptionsSubscriber extends DefaultSubscriber<Boolean> {
        private final Menu menu;

        UpdatePopupMenuOptionsSubscriber(final Menu menu) {
            this.menu = menu;
        }

        @Override
        public void onNext(Boolean offlineLikesEnabled) {
            if (offlineLikesEnabled) {
                showOfflineRemovalOption(menu);
            } else {
                showOfflineDownloadOption(menu);
            }
        }

        private void showOfflineDownloadOption(Menu menu) {
            menu.findItem(R.id.action_make_offline_available).setVisible(true);
            menu.findItem(R.id.action_make_offline_unavailable).setVisible(false);
        }

        private void showOfflineRemovalOption(Menu menu) {
            menu.findItem(R.id.action_make_offline_available).setVisible(false);
            menu.findItem(R.id.action_make_offline_unavailable).setVisible(true);
        }
    }
}

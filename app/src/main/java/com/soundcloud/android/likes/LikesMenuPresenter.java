package com.soundcloud.android.likes;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;

class LikesMenuPresenter {

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineOperations;
    private final Provider<OfflineLikesDialog> syncLikesDialogProvider;
    private final Navigator navigator;

    @Inject
    public LikesMenuPresenter(PopupMenuWrapper.Factory popupMenuWrapperFactory,
                              FeatureOperations featureOperations,
                              OfflineContentOperations offlineContentOperations,
                              Provider<OfflineLikesDialog> syncLikesDialogProvider,
                              Navigator navigator) {
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineContentOperations;
        this.syncLikesDialogProvider = syncLikesDialogProvider;
        this.navigator = navigator;
    }

    public void show(View button, final FragmentManager fragmentManager) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.likes_actions);
        menu.setOnMenuItemClickListener(getMenuWrapperListener(fragmentManager));
        configureMenu(menu);
        menu.show();
    }

    @NotNull
    private PopupMenuWrapper.PopupMenuWrapperListener getMenuWrapperListener(final FragmentManager fragmentManager) {
        return new PopupMenuWrapper.PopupMenuWrapperListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem, Context context) {
                switch (menuItem.getItemId()) {
                    case R.id.action_make_offline_available:
                        if (featureOperations.isOfflineContentEnabled()) {
                            syncLikesDialogProvider.get().show(fragmentManager);
                        } else {
                            navigator.openUpgrade(context);
                        }
                        return true;
                    case R.id.action_make_offline_unavailable:
                        offlineOperations.setOfflineLikesEnabled(false);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDismiss() { }
        };
    }

    private void configureMenu(PopupMenuWrapper menu) {
        if (offlineOperations.isOfflineLikedTracksEnabled()) {
            showOfflineRemovalOption(menu);
        } else {
            showOfflineDownloadOption(menu);
        }
    }

    private void showOfflineDownloadOption(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.action_make_offline_available, true);
        menu.setItemVisible(R.id.action_make_offline_unavailable, false);
    }

    private void showOfflineRemovalOption(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.action_make_offline_available, false);
        menu.setItemVisible(R.id.action_make_offline_unavailable, true);
    }
}

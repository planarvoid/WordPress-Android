package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

public class DefaultCastButtonInstaller implements CastButtonInstaller {

    private final CustomMediaRouteDialogFactory customMediaRouteDialogFactory;

    @Inject
    DefaultCastButtonInstaller(CustomMediaRouteDialogFactory customMediaRouteDialogFactory) {
        this.customMediaRouteDialogFactory = customMediaRouteDialogFactory;
    }

    @Override
    public Optional<MenuItem> addMediaRouterButton(Context context, Menu menu, int itemId) {
        try {
            final MenuItem menuItem = CastButtonFactory.setUpMediaRouteButton(context, menu, itemId);
            setCustomDialogFactory(menuItem);

            return Optional.of(menuItem);
        } catch (Exception ex) {
            ErrorUtils.handleSilentExceptionWithLog(ex, "Unable to set up media route item");
            return Optional.absent();
        }
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        try {
            CastButtonFactory.setUpMediaRouteButton(mediaRouteButton.getContext(), mediaRouteButton);
            mediaRouteButton.setDialogFactory(customMediaRouteDialogFactory);
        } catch (Exception ex) {
            ErrorUtils.handleSilentExceptionWithLog(ex, "Unable to set up media route item " + mediaRouteButton);
        }
    }

    private void setCustomDialogFactory(MenuItem mediaRouteMenuItem) {
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);

        mediaRouteActionProvider.setDialogFactory(customMediaRouteDialogFactory);
    }
}

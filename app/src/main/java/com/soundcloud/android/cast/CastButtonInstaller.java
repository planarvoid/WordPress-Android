package com.soundcloud.android.cast;

import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.MenuItem;

public interface CastButtonInstaller {

    Optional<MenuItem> addMediaRouterButton(Context context, Menu menu, int itemId);

    void addMediaRouterButton(MediaRouteButton mediaRouteButton);

}

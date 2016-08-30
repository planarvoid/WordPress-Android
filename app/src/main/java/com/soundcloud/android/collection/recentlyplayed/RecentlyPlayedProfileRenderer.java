package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedProfileRenderer implements CellRenderer<RecentlyPlayedPlayableItem> {

    private final boolean fixedWidth;
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;

    RecentlyPlayedProfileRenderer(boolean fixedWidth,
                                  @Provided ImageOperations imageOperations,
                                  @Provided Resources resources,
                                  @Provided Navigator navigator,
                                  @Provided ScreenProvider screenProvider,
                                  @Provided EventBus eventBus) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.collection_recently_played_profile_item_fixed_width
                     : R.layout.collection_recently_played_profile_item;

        return LayoutInflater.from(parent.getContext())
                             .inflate(layout, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<RecentlyPlayedPlayableItem> list) {
        final RecentlyPlayedPlayableItem user = list.get(position);

        setTitle(view, user.getTitle());
        setImage(view, user);
        view.setOnClickListener(goToUserProfile(user));
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setImage(View view, ImageResource imageResource) {
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        imageOperations.displayCircularInAdapterView(imageResource, getImageSize(), artwork);
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getFullImageSize(resources);
    }

    private View.OnClickListener goToUserProfile(final RecentlyPlayedPlayableItem user) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Urn urn = user.getUrn();
                Screen lastScreen = screenProvider.getLastScreen();
                eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(urn, lastScreen));
                navigator.legacyOpenProfile(view.getContext(), urn, lastScreen);
            }
        };
    }

}

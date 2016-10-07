package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;

import javax.inject.Inject;


public class NewPlaylistsPresenter extends PlaylistsPresenter {

    @LightCycle final CollapsingScrollHelper scrollHelper;
    @LightCycle final FilterHeaderPresenter filterHeaderPresenter;

    @Inject
    public NewPlaylistsPresenter(CollapsingScrollHelper scrollHelper,
                                 FilterHeaderPresenterFactory fileHeaderPresenterFactory,
                                 SwipeRefreshAttacher swipeRefreshAttacher,
                                 MyPlaylistsOperations myPlaylistsOperations,
                                 CollectionOptionsStorage collectionOptionsStorage,
                                 PlaylistsAdapter adapter,
                                 PlaylistOptionsPresenter optionsPresenter,
                                 Resources resources,
                                 EventBus eventBus) {
        super(swipeRefreshAttacher,
              myPlaylistsOperations,
              collectionOptionsStorage,
              adapter,
              optionsPresenter,
              resources,
              eventBus);

        this.scrollHelper = scrollHelper;
        this.filterHeaderPresenter = fileHeaderPresenterFactory.create(this);
    }

    @Override
    public void onRemoveFilterClicked() {
        super.onRemoveFilterClicked();
        filterHeaderPresenter.clearFilter();
    }
}

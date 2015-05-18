package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.MixedPlayableAdapter;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class UserPostsPresenter extends ListPresenter<PlayableItem> {

    private final ProfileOperations profileOperations;
    private final MixedPlayableAdapter adapter;
    private final MixedPlayableItemClickListener.Factory clickListenerFactory;
    @LightCycle final PlayableListUpdater listUpdater;

    private final Func1<PagedRemoteCollection, List<PlayableItem>> pageTransformer = new Func1<PagedRemoteCollection, List<PlayableItem>>() {
        @Override
        public List<PlayableItem> call(PagedRemoteCollection collection) {
            final List<PlayableItem> items = new ArrayList<>();
            for (PropertySet source : collection) {
                final Urn urn = source.get(EntityProperty.URN);
                if (urn.isTrack()) {
                    items.add(TrackItem.from(source));
                } else if (urn.isPlaylist()) {
                    items.add(PlaylistItem.from(source));
                }
            }
            return items;
        }
    };

    @Inject
    UserPostsPresenter(ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper,
                       ProfileOperations profileOperations, MixedPlayableAdapter adapter,
                       MixedPlayableItemClickListener.Factory clickListenerFactory, PlayableListUpdater.Factory updaterFactory) {
        super(imageOperations, pullToRefreshWrapper);
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.listUpdater = updaterFactory.create(adapter, adapter.getTrackPresenter());
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(UserPostsFragment.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedPostItems(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.pagingFunction())
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getListView().setOnItemClickListener(createClickListener(fragment));

        final String username = fragment.getArguments().getString(UserPostsFragment.USER_NAME_KEY);
        getEmptyView().setMessageText(fragment.getString(R.string.empty_user_tracks_text, username));
        getEmptyView().setImage(R.drawable.empty_sounds);
    }

    private MixedPlayableItemClickListener createClickListener(Fragment fragment) {
        final Screen screen = (Screen) fragment.getArguments().getSerializable(UserPostsFragment.SCREEN_KEY);
        final SearchQuerySourceInfo searchQuerySourceInfo = fragment.getArguments().getParcelable(UserPostsFragment.SEARCH_QUERY_SOURCE_INFO_KEY);
        return clickListenerFactory.create(screen, searchQuerySourceInfo);
    }
}

package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;
import static com.soundcloud.android.profile.ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY;
import static com.soundcloud.android.profile.ProfileArguments.USER_URN_KEY;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

abstract class ProfilePlayablePresenter extends ProfileRecyclerViewPresenter<PlayableItem> {

    protected final ProfileOperations profileOperations;
    private final MixedPlayableRecyclerItemAdapter adapter;
    private final MixedPlayableItemClickListener.Factory clickListenerFactory;
    private MixedPlayableItemClickListener clickListener;
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

    protected ProfilePlayablePresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                                       ProfileRecyclerViewScroller profileRecyclerViewScroller,
                                       MixedPlayableRecyclerItemAdapter adapter,
                                       MixedPlayableItemClickListener.Factory clickListenerFactory,
                                       PlayableListUpdater.Factory updaterFactory,
                                       ProfileOperations profileOperations) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, profileRecyclerViewScroller);
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.listUpdater = updaterFactory.create(adapter, adapter.getTrackRenderer());
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(USER_URN_KEY);
        return CollectionBinding.from(getPagedObservable(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(getPagingFunction())
                .build();
    }

    protected abstract Pager.PagingFunction<PagedRemoteCollection> getPagingFunction();

    protected abstract Observable<PagedRemoteCollection> getPagedObservable(Urn userUrn);

    protected abstract void configureEmptyView(EmptyView emptyView);

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        clickListener = createClickListener(fragment.getArguments());
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        configureEmptyView(getEmptyView());
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(adapter.getItems(), view, position);
    }

    private MixedPlayableItemClickListener createClickListener(Bundle fragmentArgs) {
        final Screen screen = (Screen) fragmentArgs.getSerializable(SCREEN_KEY);
        final SearchQuerySourceInfo searchQuerySourceInfo = fragmentArgs.getParcelable(SEARCH_QUERY_SOURCE_INFO_KEY);
        return clickListenerFactory.create(screen, searchQuerySourceInfo);
    }
}

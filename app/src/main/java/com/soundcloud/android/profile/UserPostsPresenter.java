package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerViewAdapter;
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

class UserPostsPresenter extends ProfileRecyclerViewPresenter<PlayableItem> {

    private final ProfileOperations profileOperations;
    private final MixedPlayableRecyclerViewAdapter adapter;
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

    @Inject
    UserPostsPresenter(RecyclerViewPauseOnScrollListener pauseOnScrollListener, PullToRefreshWrapper pullToRefreshWrapper,
                       ProfileOperations profileOperations, MixedPlayableRecyclerViewAdapter adapter,
                       MixedPlayableItemClickListener.Factory clickListenerFactory, PlayableListUpdater.Factory updaterFactory) {
        super(pullToRefreshWrapper, pauseOnScrollListener);
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.listUpdater = updaterFactory.create(adapter, adapter.getTrackRenderer());
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(UserPostsFragment.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedPostItems(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.postsPagingFunction())
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        clickListener = createClickListener(fragment.getArguments());
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getEmptyView().setMessageText(R.string.new_empty_user_posts_message);
        getEmptyView().setImage(R.drawable.empty_sounds);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(adapter.getItems(), view, position);
    }

    private MixedPlayableItemClickListener createClickListener(Bundle fragmentArgs) {
        final Screen screen = (Screen) fragmentArgs.getSerializable(UserPostsFragment.SCREEN_KEY);
        final SearchQuerySourceInfo searchQuerySourceInfo = fragmentArgs.getParcelable(UserPostsFragment.SEARCH_QUERY_SOURCE_INFO_KEY);
        return clickListenerFactory.create(screen, searchQuerySourceInfo);
    }
}

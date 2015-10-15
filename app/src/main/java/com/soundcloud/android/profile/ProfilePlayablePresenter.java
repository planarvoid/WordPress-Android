package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;
import static com.soundcloud.android.profile.ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

abstract class ProfilePlayablePresenter<DataT extends Iterable<PropertySet>> extends RecyclerViewPresenter<PlayableItem> {

    final MixedPlayableRecyclerItemAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    protected MixedItemClickListener clickListener;
    @LightCycle final PlayableListUpdater listUpdater;

    protected final Func1<DataT, List<PlayableItem>> pageTransformer = new Func1<DataT, List<PlayableItem>>() {
        @Override
        public List<PlayableItem> call(DataT collection) {
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
                                       MixedPlayableRecyclerItemAdapter adapter,
                                       MixedItemClickListener.Factory clickListenerFactory,
                                       PlayableListUpdater.Factory updaterFactory) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.listUpdater = updaterFactory.create(adapter, adapter.getTrackRenderer());
    }

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
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        configureEmptyView(getEmptyView());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(adapter.getItems(), view, position);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private MixedItemClickListener createClickListener(Bundle fragmentArgs) {
        final Screen screen = (Screen) fragmentArgs.getSerializable(SCREEN_KEY);
        final SearchQuerySourceInfo searchQuerySourceInfo = fragmentArgs.getParcelable(SEARCH_QUERY_SOURCE_INFO_KEY);
        return clickListenerFactory.create(screen, searchQuerySourceInfo);
    }
}

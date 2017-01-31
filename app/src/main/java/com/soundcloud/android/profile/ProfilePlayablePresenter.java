package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;
import static com.soundcloud.android.profile.ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.List;

abstract class ProfilePlayablePresenter<DataT extends Iterable<PlayableItem>>
        extends RecyclerViewPresenter<DataT, PlayableItem> {

    final MixedPlayableRecyclerItemAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    protected MixedItemClickListener clickListener;
    @LightCycle final PlayableListUpdater listUpdater;

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
        imagePauseOnScrollListener.resume();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        PlayableItem item = adapter.getItem(position);
        if (item.getUrn().isTrack()) {
            clickListener.onProfilePostClick(getPlayables(adapter), view, position, item, item.getUserUrn());
        } else {
            final Observable<List<PlayableWithReposter>> playablesWithReposters = getPlayables(adapter)
                    .map(playables -> Lists.transform(playables, PlayableWithReposter::from));
            clickListener.legacyOnPostClick(playablesWithReposters, view, position, item);
        }
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

    private Observable<List<PlayableItem>> getPlayables(final MixedPlayableRecyclerItemAdapter adapter) {
        final List<PlayableItem> items = adapter.getItems();
        return Observable.from(items).toList();
    }
}

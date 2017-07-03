package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;

import android.os.Bundle;

import javax.inject.Inject;

class UserLikesPresenter extends ProfilePlayablePresenter<PagedRemoteCollection<PlayableItem>> {

    private final UserProfileOperations operations;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    UserLikesPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                       MixedPlayableRecyclerItemAdapter adapter,
                       MixedItemClickListener.Factory clickListenerFactory,
                       PlayableListUpdater.Factory updaterFactory,
                       UserProfileOperations operations,
                       ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                       ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter, clickListenerFactory, updaterFactory);
        this.operations = operations;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    protected CollectionBinding<PagedRemoteCollection<PlayableItem>, PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = Urns.urnFromBundle(fragmentArgs, ProfileArguments.USER_URN_KEY);
        return CollectionBinding.from(operations.userLikes(userUrn))
                                .withAdapter(adapter)
                                .withPager(operations.likesPagingFunction())
                                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setImage(changeLikeToSaveExperiment.isEnabled()
                           ? R.drawable.empty_tracks_added
                           : R.drawable.empty_like);
        emptyView.setMessageText(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.USER_PROFILE_SOUNDS_LIKES_EMPTY));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

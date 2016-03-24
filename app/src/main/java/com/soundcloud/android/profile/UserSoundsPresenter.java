package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class UserSoundsPresenter extends RecyclerViewPresenter<UserProfile, UserSoundsItem> {

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserSoundsAdapter adapter;
    private final UserProfileOperations operations;
    private final UserSoundsMapper userSoundsMapper;
    private final UserSoundsItemClickListener clickListener;
    private final EventBus eventBus;
    private Urn userUrn;
    private Subscription eventSubscription = RxUtils.invalidSubscription();

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserSoundsAdapter adapter,
                        UserProfileOperations operations,
                        UserSoundsMapper userSoundsMapper,
                        UserSoundsItemClickListener clickListener,
                        EventBus eventBus) {
        super(swipeRefreshAttacher, Options.list().useDividers(Options.DividerMode.NONE).build());
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.operations = operations;
        this.userSoundsMapper = userSoundsMapper;
        this.clickListener = clickListener;
        this.eventBus = eventBus;
    }

    @Override
    protected CollectionBinding<UserProfile, UserSoundsItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);

        return CollectionBinding
                .from(operations.userProfile(userUrn), userSoundsMapper)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<UserProfile, UserSoundsItem> onRefreshBinding() {
        return CollectionBinding
                .from(operations.userProfile(userUrn), userSoundsMapper)
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);

        eventSubscription = eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter));
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        bindEmptyView(fragment.getArguments().getBoolean(UserSoundsFragment.IS_CURRENT_USER, false));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscription.unsubscribe();
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        super.onDestroyView(fragment);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(view, adapter.getItem(position));
    }

    private void bindEmptyView(boolean isCurrentUser) {
        getEmptyView().setImage(R.drawable.empty_stream);

        if (isCurrentUser) {
            getEmptyView().setMessageText(R.string.empty_you_sounds_message);
        } else {
            getEmptyView().setMessageText(R.string.empty_user_sounds_message);
        }
    }
}

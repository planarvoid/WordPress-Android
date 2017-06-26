package com.soundcloud.android.profile;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.FollowEntityListSubscriber;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class UserFollowingsPresenter extends RecyclerViewPresenter<PagedRemoteCollection<UserItem>, UserItem> {

    private final UserProfileOperations profileOperations;
    private final UserRecyclerItemAdapter adapter;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private Screen screen;
    private Subscription subscription;

    @Inject
    UserFollowingsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                            SwipeRefreshAttacher swipeRefreshAttacher,
                            UserProfileOperations profileOperations,
                            UserRecyclerItemAdapter adapter,
                            EventBus eventBus,
                            Navigator navigator) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    @Override
    protected CollectionBinding<PagedRemoteCollection<UserItem>, UserItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(UserFollowingsFragment.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedFollowings(userUrn))
                                .withAdapter(adapter)
                                .withPager(profileOperations.followingsPagingFunction())
                                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        screen = (Screen) fragment.getArguments().getSerializable(ProfileArguments.SCREEN_KEY);

        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        subscription = eventBus.subscribe(EventQueue.FOLLOWING_CHANGED, new FollowEntityListSubscriber(adapter));
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        getEmptyView().setMessageText(R.string.new_empty_user_followings_text);
        getEmptyView().setImage(R.drawable.empty_following);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        imagePauseOnScrollListener.resume();
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getUrn();
        EventContextMetadata.Builder eventContextMetadataBuilder = EventContextMetadata.builder()
                                                                                       .linkType(LinkType.SELF)
                                                                                       .module(Module.create(Module.USER_FOLLOWING, position))
                                                                                       .pageName(screen.get());

        navigator.navigateTo(getFragmentActivity(view), NavigationTarget.forProfile(urn,
                                                         Optional.of(UIEvent.fromNavigation(urn, eventContextMetadataBuilder.build())),
                                                         Optional.absent(),
                                                         Optional.absent()));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

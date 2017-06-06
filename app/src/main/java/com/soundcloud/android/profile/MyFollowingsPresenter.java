package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.PrependItemToListObserver;
import com.soundcloud.android.view.adapters.RemoveEntityListObserver;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import com.soundcloud.java.collections.Lists;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import org.jetbrains.annotations.Nullable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class MyFollowingsPresenter extends RecyclerViewPresenter<List<Following>, UserItem> {

    private final MyProfileOperations profileOperations;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final FollowingOperations followingOperations;
    private final EntityItemCreator entityItemCreator;

    private final Function<List<Following>, List<UserItem>> pageTransformer = new Function<List<Following>, List<UserItem>>() {
        @Override
        public List<UserItem> apply(List<Following> collection) {
            return Lists.transform(collection, following -> entityItemCreator.userItem(following.user()));
        }
    };

    private final UserRecyclerItemAdapter adapter;
    private final NavigationExecutor navigationExecutor;
    private Screen screen;
    private CompositeDisposable updateFollowingsSubscription;

    @Inject
    MyFollowingsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                          ImagePauseOnScrollListener imagePauseOnScrollListener,
                          UserRecyclerItemAdapter adapter,
                          MyProfileOperations profileOperations,
                          FollowingOperations followingOperations,
                          EntityItemCreator entityItemCreator, NavigationExecutor navigationExecutor) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.profileOperations = profileOperations;
        this.followingOperations = followingOperations;
        this.entityItemCreator = entityItemCreator;
        this.navigationExecutor = navigationExecutor;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        screen = (Screen) fragment.getArguments().getSerializable(SCREEN_KEY);

        updateFollowingsSubscription = new CompositeDisposable(

                followingOperations.populatedOnUserFollowed()
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribeWith(new PrependItemToListObserver<>(adapter)),

                followingOperations.onUserUnfollowed()
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribeWith(new RemoveEntityListObserver(adapter))
        );

        getBinding().connect();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        updateFollowingsSubscription.clear();
        super.onDestroy(fragment);
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
    protected CollectionBinding<List<Following>, UserItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.fromV2(profileOperations.followings(), pageTransformer)
                                .withAdapter(adapter)
                                .withPager(profileOperations.followingsPagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<Following>, UserItem> onRefreshBinding() {
        return CollectionBinding.fromV2(profileOperations.updatedFollowings(), pageTransformer)
                                .withAdapter(adapter)
                                .withPager(profileOperations.followingsPagingFunction())
                                .build();
    }

    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.list_empty_you_following_message);
        emptyView.setImage(R.drawable.empty_following);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getUrn();
        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                                                                        .pageName(screen.get())
                                                                        .linkType(LinkType.SELF)
                                                                        .module(Module.create(Module.USER_FOLLOWING, position))
                                                                        .build();

        navigationExecutor.openProfile(view.getContext(), urn, UIEvent.fromNavigation(urn, eventContextMetadata));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}

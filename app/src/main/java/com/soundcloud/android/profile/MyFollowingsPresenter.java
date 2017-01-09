package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class MyFollowingsPresenter extends RecyclerViewPresenter<List<PropertySet>, UserItem> {

    private final MyProfileOperations profileOperations;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final FollowingOperations followingOperations;

    private final Func1<List<PropertySet>, List<UserItem>> pageTransformer = collection -> {
        final List<UserItem> items = new ArrayList<>();
        for (PropertySet source : collection) {
            items.add(UserItem.from(source));
        }
        return items;
    };

    private final UserRecyclerItemAdapter adapter;
    private final Navigator navigator;
    private Screen screen;
    private CompositeSubscription updateFollowingsSubscription;


    @Inject
    MyFollowingsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                          ImagePauseOnScrollListener imagePauseOnScrollListener,
                          UserRecyclerItemAdapter adapter,
                          MyProfileOperations profileOperations,
                          FollowingOperations followingOperations,
                          Navigator navigator) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.profileOperations = profileOperations;
        this.followingOperations = followingOperations;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        screen = (Screen) fragment.getArguments().getSerializable(SCREEN_KEY);

        updateFollowingsSubscription = new CompositeSubscription(

                followingOperations.populatedOnUserFollowed()
                                   .map(UserItem.fromPropertySet())
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .subscribe(new PrependItemToListSubscriber<>(adapter)),

                followingOperations.onUserUnfollowed()
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .subscribe(new RemoveEntityListSubscriber(adapter))
        );

        getBinding().connect();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        updateFollowingsSubscription.unsubscribe();
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
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<List<PropertySet>, UserItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(profileOperations.pagedFollowings(), pageTransformer)
                                .withAdapter(adapter)
                                .withPager(profileOperations.followingsPagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<PropertySet>, UserItem> onRefreshBinding() {
        return CollectionBinding.from(profileOperations.updatedFollowings(), pageTransformer)
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
                                                                        .contextScreen(screen.get())
                                                                        .build();

        navigator.openProfile(view.getContext(), urn, UIEvent.fromNavigation(urn, eventContextMetadata));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}

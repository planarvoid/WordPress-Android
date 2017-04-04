package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
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
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class UserFollowersPresenter extends RecyclerViewPresenter<PagedRemoteCollection<UserItem>, UserItem> {

    private final UserProfileOperations profileOperations;
    private final UserRecyclerItemAdapter adapter;
    private final Navigator navigator;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private Screen screen;

    @Inject
    UserFollowersPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                           SwipeRefreshAttacher swipeRefreshAttacher,
                           UserProfileOperations profileOperations,
                           UserRecyclerItemAdapter adapter,
                           Navigator navigator) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.navigator = navigator;
    }

    @Override
    protected CollectionBinding<PagedRemoteCollection<UserItem>, UserItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedFollowers(userUrn))
                                .withAdapter(adapter)
                                .withPager(profileOperations.followersPagingFunction())
                                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        screen = (Screen) fragment.getArguments().getSerializable(ProfileArguments.SCREEN_KEY);

        getBinding().connect();
    }


    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        getEmptyView().setImage(R.drawable.empty_followers);
        setEmtpyViewMessage(fragment);
    }

    private void setEmtpyViewMessage(Fragment fragment) {
        final boolean isCurrentUser = fragment.getArguments().getBoolean(UserFollowersFragment.IS_CURRENT_USER, false);
        if (isCurrentUser) {
            getEmptyView().setMessageText(R.string.list_empty_you_followers_message);
            getEmptyView().setSecondaryText(R.string.list_empty_you_followers_secondary);
        } else {
            getEmptyView().setMessageText(R.string.new_empty_user_followers_text);
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        imagePauseOnScrollListener.resume();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getUrn();
        EventContextMetadata.Builder eventContextMetadataBuilder = EventContextMetadata.builder()
                                                                                       .linkType(LinkType.SELF)
                                                                                       .module(Module.create(Module.USER_FOLLOWERS, position));

        if (screen != null) {
            eventContextMetadataBuilder.pageName(screen.get());
        }

        navigator.openProfile(view.getContext(), urn, UIEvent.fromNavigation(urn, eventContextMetadataBuilder.build()));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

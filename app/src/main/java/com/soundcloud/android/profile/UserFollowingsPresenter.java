package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class UserFollowingsPresenter extends RecyclerViewPresenter<UserItem> {

    private final UserProfileOperations profileOperations;
    private final UserRecyclerItemAdapter adapter;
    private final Navigator navigator;

    private final Func1<PagedRemoteCollection, List<UserItem>> pageTransformer = new Func1<PagedRemoteCollection, List<UserItem>>() {
        @Override
        public List<UserItem> call(PagedRemoteCollection collection) {
            final List<UserItem> items = new ArrayList<>();
            for (PropertySet source : collection) {
                items.add(UserItem.from(source));
            }
            return items;
        }
    };
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;

    @Inject
    UserFollowingsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener, SwipeRefreshAttacher swipeRefreshAttacher,
                            UserProfileOperations profileOperations, UserRecyclerItemAdapter adapter, Navigator navigator) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.profileOperations = profileOperations;
        this.adapter = adapter;
        this.navigator = navigator;
    }

    @Override
    protected CollectionBinding<UserItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(UserPostsFragment.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedFollowings(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.followingsPagingFunction())
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
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
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        navigator.openProfile(view.getContext(), adapter.getItem(position).getEntityUrn());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

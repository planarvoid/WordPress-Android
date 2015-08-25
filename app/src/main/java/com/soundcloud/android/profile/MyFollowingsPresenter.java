package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SCREEN_KEY;
import static com.soundcloud.android.profile.ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class MyFollowingsPresenter extends RecyclerViewPresenter<UserItem> {

    private final MyProfileOperations profileOperations;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;

    private final Func1<List<PropertySet>, List<UserItem>> pageTransformer = new Func1<List<PropertySet>, List<UserItem>>() {
        @Override
        public List<UserItem> call(List<PropertySet> collection) {
            final List<UserItem> items = new ArrayList<>();
            for (PropertySet source : collection) {
                items.add(UserItem.from(source));
            }
            return items;
        }
    };

    private final UserRecyclerItemAdapter adapter;
    private final Navigator navigator;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private Screen screen;


    @Inject
    MyFollowingsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                          ImagePauseOnScrollListener imagePauseOnScrollListener,
                          UserRecyclerItemAdapter adapter,
                          MyProfileOperations profileOperations, Navigator navigator) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.profileOperations = profileOperations;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);

        screen = (Screen) fragment.getArguments().getSerializable(SCREEN_KEY);
        searchQuerySourceInfo = fragment.getArguments().getParcelable(SEARCH_QUERY_SOURCE_INFO_KEY);

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
    protected CollectionBinding<UserItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(profileOperations.pagedFollowings(), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.followingsPagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<UserItem> onRefreshBinding() {
        return CollectionBinding.from(profileOperations.updatedFollowings(), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.followingsPagingFunction())
                .build();
    }

    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.list_empty_user_following_message);
        emptyView.setImage(R.drawable.empty_following);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        navigator.openProfile(view.getContext(), adapter.getItem(position).getEntityUrn(), screen, searchQuerySourceInfo);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}

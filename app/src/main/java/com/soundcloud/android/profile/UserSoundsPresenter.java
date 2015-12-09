package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UserRecyclerItemAdapter;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class UserSoundsPresenter extends RecyclerViewPresenter<UserItem> {

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserRecyclerItemAdapter adapter;

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserRecyclerItemAdapter adapter) {
        super(swipeRefreshAttacher);
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
    }

    @Override
    protected CollectionBinding<UserItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(Observable.<UserItem>empty().toList())
                .withAdapter(adapter)
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
        bindEmptyView(fragment.getArguments().getBoolean(UserSoundsFragment.IS_CURRENT_USER, false));
    }

    private void bindEmptyView(boolean isCurrentUser) {
        getEmptyView().setImage(R.drawable.empty_stream);

        if (isCurrentUser) {
            getEmptyView().setMessageText(R.string.empty_you_sounds_message);
        } else {
            getEmptyView().setMessageText(R.string.empty_user_sounds_message);
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {}

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

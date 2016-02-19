package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.Collections;

class UserSoundsPresenter extends RecyclerViewPresenter<UserSoundsItem> {

    private final MixedItemClickListener itemClickListener;

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserSoundsAdapter adapter;
    private final UserProfileOperations operations;
    private UserSoundsMapper userSoundsMapper;
    private Urn userUrn;

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserSoundsAdapter adapter,
                        UserProfileOperations operations,
                        MixedItemClickListener.Factory itemClickListenerFactory,
                        UserSoundsMapper userSoundsMapper) {
        super(swipeRefreshAttacher, Options.list().useDividers(Options.DividerMode.NONE).build());
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.operations = operations;
        this.userSoundsMapper = userSoundsMapper;
        this.itemClickListener = itemClickListenerFactory.create(Screen.USER_SOUNDS, null);
    }

    @Override
    protected CollectionBinding<UserSoundsItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);

        return CollectionBinding
                .from(operations.userProfile(userUrn).map(userSoundsMapper))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<UserSoundsItem> onRefreshBinding() {
        return CollectionBinding
                .from(operations.userProfile(userUrn).map(userSoundsMapper))
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
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        bindEmptyView(fragment.getArguments().getBoolean(UserSoundsFragment.IS_CURRENT_USER, false));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        super.onDestroyView(fragment);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        // In the future, this method should gather the playables from the list of items in the adapter
        // and forward them to the mixedItemClickListener.
        // Note: The mixed item click listener may need additional love to play through both tracks and playlists, as
        // that is now supported by the playback functionality.

        itemClickListener.onItemClick(Collections.<ListItem>emptyList(), view, position);
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

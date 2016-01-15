package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class UserSoundsPresenter extends RecyclerViewPresenter<UserSoundsBucket> {
    private static PagedRemoteCollection SOURCE_TO_PAGED_REMOTE_COLLECTION(ModelCollection<? extends ApiEntityHolderSource> modelCollection) {
        return new PagedRemoteCollection(TO_PROPERTY_SET_SOURCES(modelCollection));
    }

    private static PagedRemoteCollection TO_PAGED_REMOTE_COLLECTION(ModelCollection<? extends PropertySetSource> modelCollection) {
        return new PagedRemoteCollection(modelCollection);
    }

    private static ModelCollection<PropertySetSource> TO_PROPERTY_SET_SOURCES(ModelCollection<? extends ApiEntityHolderSource> entityHolderSources) {
        List<PropertySetSource> entities = new ArrayList<>();

        for (ApiEntityHolderSource entityHolderSource : entityHolderSources) {
            final Optional<ApiEntityHolder> entityHolder = entityHolderSource.getEntityHolder();

            if (entityHolder.isPresent()) {
                entities.add(entityHolder.get());
            }
        }

        return new ModelCollection<>(entities, entityHolderSources.getLinks());
    }

    private final Func1<UserProfileRecord, Iterable<UserSoundsBucket>> TO_BUCKETS = new Func1<UserProfileRecord, Iterable<UserSoundsBucket>>() {
        @Override
        public Iterable<UserSoundsBucket> call(UserProfileRecord userProfile) {
            final List<UserSoundsBucket> buckets = new ArrayList<>();
            final ModelCollection<? extends ApiEntityHolderSource> spotlight = userProfile.getSpotlight();
            final ModelCollection<? extends ApiEntityHolder> tracks = userProfile.getTracks();
            final ModelCollection<? extends ApiEntityHolder> releases = userProfile.getReleases();
            final ModelCollection<? extends ApiEntityHolder> playlists = userProfile.getPlaylists();
            final ModelCollection<? extends ApiEntityHolderSource> reposts = userProfile.getReposts();
            final ModelCollection<? extends ApiEntityHolderSource> likes = userProfile.getLikes();

            if (!spotlight.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_spotlight_heading), UserSoundsTypes.SPOTLIGHT, SOURCE_TO_PAGED_REMOTE_COLLECTION(spotlight)));
            }

            if (!tracks.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_tracks_heading), UserSoundsTypes.TRACKS, TO_PAGED_REMOTE_COLLECTION(tracks)));
            }

            if (!releases.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_releases_heading), UserSoundsTypes.RELEASES, TO_PAGED_REMOTE_COLLECTION(releases)));
            }

            if (!playlists.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_playlists_heading), UserSoundsTypes.PLAYLISTS, TO_PAGED_REMOTE_COLLECTION(playlists)));
            }

            if (!reposts.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_reposts_heading), UserSoundsTypes.REPOSTS, SOURCE_TO_PAGED_REMOTE_COLLECTION(reposts)));
            }

            if (!likes.getCollection().isEmpty()) {
                buckets.add(UserSoundsBucket.create(resources.getString(R.string.user_profile_likes_heading), UserSoundsTypes.LIKES, SOURCE_TO_PAGED_REMOTE_COLLECTION(likes)));
            }

            return buckets;
        }
    };

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserSoundsAdapter adapter;
    private final UserProfileOperations operations;
    private final Resources resources;
    private Urn userUrn;

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserSoundsAdapter adapter,
                        UserProfileOperations operations,
                        Resources resources) {
        super(swipeRefreshAttacher, Options.custom().useDividers(Options.DividerMode.NONE).build());
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.operations = operations;
        this.resources = resources;
    }

    @Override
    protected CollectionBinding<UserSoundsBucket> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);

        return CollectionBinding
                .from(operations.userProfile(userUrn).map(TO_BUCKETS))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<UserSoundsBucket> onRefreshBinding() {
        return CollectionBinding
                .from(operations.userProfile(userUrn).map(TO_BUCKETS))
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
    protected void onItemClicked(View view, int position) {
        System.out.println("Hello");
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}

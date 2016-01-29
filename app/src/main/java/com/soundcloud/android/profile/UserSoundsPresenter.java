package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class UserSoundsPresenter extends RecyclerViewPresenter<UserSoundsItem> {

    private final MixedItemClickListener itemClickListener;

    private Collection<UserSoundsItem> HOLDER_SOURCE_COLLECTION_TO_USER_SOUND_ITEMS(ModelCollection<? extends ApiEntityHolderSource> modelCollection, int collectionType) {
        List<UserSoundsItem> items = new ArrayList<>();

        items.add(UserSoundsItem.fromHeader(collectionType));

        items.addAll(Lists.transform(
                modelCollection.getCollection(),
                HOLDER_SOURCE_TO_USER_SOUND_ITEMS(collectionType)));

        if (modelCollection.getNextLink().isPresent()) {
            items.add(UserSoundsItem.fromViewAll(collectionType));
        }

        items.add(UserSoundsItem.fromDivider());

        return items;
    }

    private static Function<ApiEntityHolderSource, UserSoundsItem> HOLDER_SOURCE_TO_USER_SOUND_ITEMS(final int collectionType) {
        return new Function<ApiEntityHolderSource, UserSoundsItem>() {
            @Override
            public UserSoundsItem apply(ApiEntityHolderSource entityHolderSource) {
                final Optional<ApiEntityHolder> entityHolder = entityHolderSource.getEntityHolder();

                if (entityHolder.isPresent()) {
                    final PropertySet properties = entityHolder.get().toPropertySet();

                    if (properties.get(EntityProperty.URN).isTrack()) {
                        return UserSoundsItem.fromTrackItem(TrackItem.from(properties), collectionType);
                    } else {
                        return UserSoundsItem.fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                    }
                }

                return null;
            }
        };
    }


    private static Function<ApiEntityHolder, UserSoundsItem> HOLDER_TO_USER_SOUND_ITEMS(final int collectionType) {
        return new Function<ApiEntityHolder, UserSoundsItem>() {
            @Override
            public UserSoundsItem apply(ApiEntityHolder holder) {
                final PropertySet properties = holder.toPropertySet();

                if (properties.get(EntityProperty.URN).isTrack()) {
                    final UserSoundsItem userSoundsItem = UserSoundsItem.fromTrackItem(TrackItem.from(properties), collectionType);
                    return userSoundsItem;
                } else {
                    return UserSoundsItem.fromPlaylistItem(PlaylistItem.from(properties), collectionType);
                }
            }
        };
    }

    private final Func1<UserProfileRecord, Iterable<UserSoundsItem>> TO_USER_SOUND_ITEMS = new Func1<UserProfileRecord, Iterable<UserSoundsItem>>() {
        @Override
        public Iterable<UserSoundsItem> call(UserProfileRecord userProfile) {
            final List<UserSoundsItem> items = new ArrayList<>();

            final ModelCollection<? extends ApiEntityHolderSource> spotlight = userProfile.getSpotlight();
            final ModelCollection<? extends ApiEntityHolder> tracks = userProfile.getTracks();
            final ModelCollection<? extends ApiEntityHolder> releases = userProfile.getReleases();
            final ModelCollection<? extends ApiEntityHolder> playlists = userProfile.getPlaylists();
            final ModelCollection<? extends ApiEntityHolderSource> reposts = userProfile.getReposts();
            final ModelCollection<? extends ApiEntityHolderSource> likes = userProfile.getLikes();

            if (!spotlight.getCollection().isEmpty()) {
                items.addAll(HOLDER_SOURCE_COLLECTION_TO_USER_SOUND_ITEMS(spotlight, UserSoundsTypes.SPOTLIGHT));
            }

            if (!tracks.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromHeader(UserSoundsTypes.TRACKS));

                items.addAll(Lists.transform(
                        tracks.getCollection(),
                        HOLDER_TO_USER_SOUND_ITEMS(UserSoundsTypes.TRACKS)));

                if (tracks.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.TRACKS));
                }

                items.add(UserSoundsItem.fromDivider());
            }

            if (!releases.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromHeader(UserSoundsTypes.RELEASES));

                items.addAll(Lists.transform(
                        releases.getCollection(),
                        HOLDER_TO_USER_SOUND_ITEMS(UserSoundsTypes.RELEASES)));

                if (releases.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.RELEASES));
                }

                items.add(UserSoundsItem.fromDivider());
            }

            if (!playlists.getCollection().isEmpty()) {
                items.add(UserSoundsItem.fromHeader(UserSoundsTypes.PLAYLISTS));

                items.addAll(Lists.transform(
                        playlists.getCollection(),
                        HOLDER_TO_USER_SOUND_ITEMS(UserSoundsTypes.PLAYLISTS)));

                if (playlists.getNextLink().isPresent()) {
                    items.add(UserSoundsItem.fromViewAll(UserSoundsTypes.PLAYLISTS));
                }

                items.add(UserSoundsItem.fromDivider());
            }

            if (!reposts.getCollection().isEmpty()) {
                items.addAll(HOLDER_SOURCE_COLLECTION_TO_USER_SOUND_ITEMS(reposts, UserSoundsTypes.REPOSTS));
            }

            if (!likes.getCollection().isEmpty()) {
                items.addAll(HOLDER_SOURCE_COLLECTION_TO_USER_SOUND_ITEMS(likes, UserSoundsTypes.LIKES));
            }

            return items;
        }
    };

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserSoundsAdapter adapter;
    private final UserProfileOperations operations;
    private final Resources resources;
    private final MixedItemClickListener.Factory itemClickListenerFactory;
    private Urn userUrn;

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserSoundsAdapter adapter,
                        UserProfileOperations operations,
                        Resources resources,
                        MixedItemClickListener.Factory itemClickListenerFactory) {
        super(swipeRefreshAttacher, Options.list().useDividers(Options.DividerMode.NONE).build());
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.operations = operations;
        this.resources = resources;
        this.itemClickListenerFactory = itemClickListenerFactory;
        this.itemClickListener = itemClickListenerFactory.create(Screen.USER_SOUNDS, null);
    }

    @Override
    protected CollectionBinding<UserSoundsItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);

        return CollectionBinding
                .from(operations.userProfile(userUrn).map(TO_USER_SOUND_ITEMS))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<UserSoundsItem> onRefreshBinding() {
        return CollectionBinding
                .from(operations.userProfile(userUrn).map(TO_USER_SOUND_ITEMS))
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
}

package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY;
import static com.soundcloud.android.profile.UserSoundsItem.getPositionInModule;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class UserSoundsPresenter extends RecyclerViewPresenter<UserProfile, UserSoundsItem> {

    private static final Function<UserSoundsItem, PlayableItem> USER_SOUNDS_ITEM_TO_PLAYABLE_ITEM = new Function<UserSoundsItem, PlayableItem>() {
        @Override
        public PlayableItem apply(final UserSoundsItem userSoundsItem) {
            return userSoundsItem.getPlayableItem().orNull();
        }
    };

    private static final Predicate<UserSoundsItem> FILTER_PLAYABLE_USER_SOUNDS_ITEMS = new Predicate<UserSoundsItem>() {
        @Override
        public boolean apply(UserSoundsItem input) {
            return input.isPlaylist() || input.isTrack();
        }
    };

    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final UserSoundsAdapter adapter;
    private final UserProfileOperations operations;
    private final UserSoundsItemMapper userSoundsItemMapper;
    private final UserSoundsItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;
    private final Resources resources;
    private Urn userUrn;
    private Subscription userSubscription = RxUtils.invalidSubscription();
    private CompositeSubscription viewLifeCycle;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private UserSoundsItemClickListener clickListener;

    @Inject
    UserSoundsPresenter(ImagePauseOnScrollListener imagePauseOnScrollListener,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        UserSoundsAdapter adapter,
                        UserProfileOperations operations,
                        UserSoundsItemMapper userSoundsItemMapper,
                        UserSoundsItemClickListener.Factory clickListenerFactory,
                        EventBus eventBus,
                        Resources resources) {
        super(swipeRefreshAttacher, Options.staggeredGrid(R.integer.user_profile_card_grid_span_count)
                                           .useDividers(Options.DividerMode.NONE)
                                           .build());
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
        this.adapter = adapter;
        this.operations = operations;
        this.userSoundsItemMapper = userSoundsItemMapper;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
        this.resources = resources;
    }

    @Override
    protected CollectionBinding<UserProfile, UserSoundsItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);

        return CollectionBinding
                .from(operations.userProfile(userUrn), userSoundsItemMapper)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<UserProfile, UserSoundsItem> onRefreshBinding() {
        return CollectionBinding
                .from(operations.userProfile(userUrn), userSoundsItemMapper)
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        userUrn = fragment.getArguments().getParcelable(ProfileArguments.USER_URN_KEY);
        searchQuerySourceInfo = fragment.getArguments().getParcelable(SEARCH_QUERY_SOURCE_INFO_KEY);
        clickListener = this.clickListenerFactory.create(searchQuerySourceInfo);
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.REPOST_CHANGED, new RepostEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        configureEmptyView(getEmptyView(), fragment);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        userSubscription.unsubscribe();
        getRecyclerView().removeOnScrollListener(imagePauseOnScrollListener);
        super.onDestroyView(fragment);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final List<UserSoundsItem> userSoundsItems = adapter.getItems();
        final UserSoundsItem clickedItem = adapter.getItem(position);

        if (!clickedItem.isDivider()) {
            final List<PlayableItem> playables = filterPlayableItems(userSoundsItems);
            final int playablePosition = filterPlayableItems(userSoundsItems.subList(0, position)).size();

            clickListener.onItemClick(Observable.just(playables),
                                      view,
                                      playablePosition,
                                      clickedItem,
                                      userUrn,
                                      searchQuerySourceInfo,
                                      fromModule(clickedItem.getCollectionType(),
                                                 getPositionInModule(userSoundsItems, clickedItem)));
        }
    }

    private void displaySecondaryTextForOtherUser() {
        userSubscription.unsubscribe();
        userSubscription = operations.getLocalProfileUser(userUrn)
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .subscribe(new DefaultSubscriber<ProfileUser>() {
                                         @Override
                                         public void onNext(ProfileUser profileUser) {
                                             getEmptyView().setSecondaryText(
                                                     resources.getString(R.string.empty_user_sounds_message_secondary,
                                                                         profileUser.getName())
                                             );
                                         }
                                     });
    }

    private void configureEmptyView(final EmptyView emptyView, final Fragment fragment) {
        final Boolean isCurrentUser = fragment.getArguments().getBoolean(UserSoundsFragment.IS_CURRENT_USER, false);

        emptyView.setImage(R.drawable.empty_lists_sounds);

        if (isCurrentUser) {
            emptyView.setMessageText(R.string.empty_you_sounds_message);
            emptyView.setSecondaryText(R.string.empty_you_sounds_message_secondary);
        } else {
            emptyView.setMessageText(R.string.empty_user_sounds_message);
            displaySecondaryTextForOtherUser();
        }
    }

    private List<PlayableItem> filterPlayableItems(final List<UserSoundsItem> userSoundsItems) {
        return transform(newArrayList(filter(userSoundsItems, FILTER_PLAYABLE_USER_SOUNDS_ITEMS)),
                         USER_SOUNDS_ITEM_TO_PLAYABLE_ITEM);
    }
}

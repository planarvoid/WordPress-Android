package com.soundcloud.android.collection.recentlyplayed;

import static butterknife.ButterKnife.findById;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.image.StyledImageView;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.users.UserMenuPresenter;
import com.soundcloud.android.utils.OverflowButtonBackground;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.OverflowAnchorImageView;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedProfileRenderer implements CellRenderer<RecentlyPlayedPlayableItem> {

    private final boolean fixedWidth;
    private final ImageOperations imageOperations;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;
    private final UserMenuPresenter userMenuPresenter;

    RecentlyPlayedProfileRenderer(boolean fixedWidth,
                                  @Provided ImageOperations imageOperations,
                                  @Provided Navigator navigator,
                                  @Provided ScreenProvider screenProvider,
                                  @Provided EventBus eventBus,
                                  @Provided UserMenuPresenter userMenuPresenter) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        this.userMenuPresenter = userMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.collection_recently_played_profile_item_fixed_width
                     : R.layout.collection_recently_played_profile_item_variable_width;

        return LayoutInflater.from(parent.getContext())
                             .inflate(layout, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<RecentlyPlayedPlayableItem> list) {
        final RecentlyPlayedPlayableItem user = list.get(position);

        setTitle(view, user.getTitle());
        setImage(view, user);
        view.setOnClickListener(goToUserProfile(user));
        setupOverflow(findById(view, R.id.overflow_button), user, position);
    }

    private void setupOverflow(final OverflowAnchorImageView button, final RecentlyPlayedPlayableItem user, final int position) {
        button.setOnClickListener(v -> userMenuPresenter.show(button, user.getUrn(), EventContextMetadata.builder()
                                                                                                 .pageName(screenProvider.getLastScreen().get())
                                                                                                 .module(Module.create(Module.RECENTLY_PLAYED, position))
                                                                                                 .build()));
        OverflowButtonBackground.install(button, R.dimen.collection_recently_played_item_overflow_menu_padding);
        ViewUtils.extendTouchArea(button, R.dimen.collection_recently_played_item_overflow_menu_touch_expansion);
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setImage(View view, ImageResource imageResource) {
        final StyledImageView styledImageView = findById(view, R.id.artwork);
        styledImageView.showWithoutPlaceholder(imageResource.getImageUrlTemplate(), Optional.of(ImageStyle.CIRCULAR), imageResource.getUrn().toString(), imageOperations);
    }

    private View.OnClickListener goToUserProfile(final RecentlyPlayedPlayableItem user) {
        return view -> {
            Urn urn = user.getUrn();
            Screen lastScreen = screenProvider.getLastScreen();
            eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(urn, lastScreen));
            navigator.navigateTo(getFragmentActivity(view), NavigationTarget.forProfile(urn, Optional.absent(), Optional.of(lastScreen), Optional.absent()));
        };
    }

}

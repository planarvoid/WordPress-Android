package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class FollowableUserItemRenderer extends UserItemRenderer {

    private final FollowingOperations followingOperations;
    private final EngagementsTracking engagementsTracking;
    private final ScreenProvider screenProvider;

    @Inject
    public FollowableUserItemRenderer(ImageOperations imageOperations,
                                      CondensedNumberFormatter numberFormatter,
                                      FollowingOperations followingOperations,
                                      EngagementsTracking engagementsTracking,
                                      ScreenProvider screenProvider) {
        super(imageOperations, numberFormatter);
        this.followingOperations = followingOperations;
        this.engagementsTracking = engagementsTracking;
        this.screenProvider = screenProvider;
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        this.bindItemView(position, itemView, items.get(position));
    }

    public void bindItemView(int position, View itemView, UserItem user) {
        bindItemView(position, itemView, user, Optional.absent());
    }

    public void bindItemView(int position, View itemView, UserItem user, Optional<String> clickSource) {
        super.bindItemView(itemView, user);
        setupFollowToggle(itemView, user, position, clickSource);
    }

    private void setupFollowToggle(View itemView, final UserItem user, final int position, Optional<String> clickSource) {
        final ToggleButton toggleFollow = ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));
        toggleFollow.setVisibility(View.VISIBLE);
        toggleFollow.setChecked(user.isFollowedByMe());
        toggleFollow.setOnClickListener(v -> {
            final String screen = screenProvider.getLastScreen().get();

            fireAndForget(followingOperations.toggleFollowing(user.getUrn(), toggleFollow.isChecked()));

            engagementsTracking.followUserUrn(user.getUrn(),
                                              toggleFollow.isChecked(),
                                              getEventContextMetadata(position, screen, clickSource));
        });
    }

    private EventContextMetadata getEventContextMetadata(int position, String screen, Optional<String> clickSource) {
        return EventContextMetadata.builder()
                                   .module(Module.create(screen, position))
                                   .pageName(screen)
                                   .clickSource(clickSource).build();
    }
}

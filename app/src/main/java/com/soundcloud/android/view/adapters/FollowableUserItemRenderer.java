package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.users.UserItem;

import android.view.View;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class FollowableUserItemRenderer extends UserItemRenderer {
    private final NextFollowingOperations followingOperations;

    @Inject
    public FollowableUserItemRenderer(ImageOperations imageOperations, NextFollowingOperations followingOperations) {
        super(imageOperations);
        this.followingOperations = followingOperations;
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        super.bindItemView(position, itemView, items);
        setupFollowToggle(itemView, items.get(position));
    }

    private void setupFollowToggle(View itemView, final UserItem user) {
        final ToggleButton toggleFollow = ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));

        toggleFollow.setVisibility(View.VISIBLE);
        toggleFollow.setChecked(user.isFollowedByMe());
        toggleFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fireAndForget(followingOperations.toggleFollowing(user.getEntityUrn(), toggleFollow.isChecked()));
            }
        });
    }
}

package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.view.View;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class FollowableUserItemRenderer extends UserItemRenderer {

    private final NextFollowingOperations followingOperations;
    private final FeatureFlags featureFlags;

    @Inject
    public FollowableUserItemRenderer(ImageOperations imageOperations, CondensedNumberFormatter numberFormatter,
                                      NextFollowingOperations followingOperations, FeatureFlags featureFlags) {
        super(imageOperations, numberFormatter);
        this.followingOperations = followingOperations;
        this.featureFlags = featureFlags;
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserItem> items) {
        super.bindItemView(position, itemView, items);
        setupFollowToggle(itemView, items.get(position));
    }

    private void setupFollowToggle(View itemView, final UserItem user) {
        final ToggleButton toggleFollow = ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));

        if(featureFlags.isEnabled(Flag.FOLLOW_USER_SEARCH)) {
            toggleFollow.setVisibility(View.VISIBLE);
            toggleFollow.setChecked(user.isFollowedByMe());
            toggleFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fireAndForget(followingOperations.toggleFollowing(user.getEntityUrn(), toggleFollow.isChecked()));
                }
            });
        } else {
            toggleFollow.setVisibility(View.GONE);
        }
    }
}

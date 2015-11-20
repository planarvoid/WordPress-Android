package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.util.Collections;
import java.util.Locale;

public class FollowableUserItemRendererTest extends AndroidUnitTest {

    private FollowableUserItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private FollowingOperations followingOperations;
    @Mock private EngagementsTracking engagementsTracking;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    private View itemView;
    private ApiUser user;

    @Before
    public void setup() {
        renderer = new FollowableUserItemRenderer(imageOperations, numberFormatter, followingOperations, engagementsTracking);

        itemView = LayoutInflater.from(context()).inflate(R.layout.user_list_item, new FrameLayout(context()), false);
        user = ModelFixtures.create(ApiUser.class);
    }

    @Test
    public void shouldSetFollowedToggleButton() {
        UserItem followedUserItem = UserItem.from(TestPropertySets.userFollowing(user, true));
        renderer.bindItemView(0, itemView, Collections.singletonList(followedUserItem));

        ToggleButton followButton = getFollowToggleButton();
        assertThat(followButton.isChecked()).isTrue();
        assertThat(followButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldNotSetFollowedToggleButton() {
        UserItem unfollowedUserItem = UserItem.from(TestPropertySets.userFollowing(user, false));
        renderer.bindItemView(0, itemView, Collections.singletonList(unfollowedUserItem));

        ToggleButton followButton = getFollowToggleButton();
        assertThat(followButton.isChecked()).isFalse();
        assertThat(followButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private ToggleButton getFollowToggleButton() {
        return ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));
    }
}

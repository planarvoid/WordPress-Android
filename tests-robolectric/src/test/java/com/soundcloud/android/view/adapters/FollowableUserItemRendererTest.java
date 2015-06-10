package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class FollowableUserItemRendererTest {

    @InjectMocks private FollowableUserItemRenderer renderer;

    @Mock private FeatureFlags featureFlags;
    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private NextFollowingOperations followingOperations;

    private View itemView;
    private ApiUser user;

    @Before
    public void setup() {
        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.user_list_item, new FrameLayout(context), false);
        user = ModelFixtures.create(ApiUser.class);
        
        when(featureFlags.isEnabled(Flag.FOLLOW_USER_SEARCH)).thenReturn(true);
    }

    @Test
    public void shouldSetFollowedToggleButton() {
        UserItem followedUserItem = UserItem.from(TestPropertySets.userFollowing(user, true));
        renderer.bindItemView(0, itemView, Arrays.asList(followedUserItem));

        ToggleButton followButton = getFollowToggleButton();
        expect(followButton.isChecked()).toEqual(true);
        expect(followButton.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldNotSetFollowedToggleButton() {
        UserItem unfollowedUserItem = UserItem.from(TestPropertySets.userFollowing(user, false));
        renderer.bindItemView(0, itemView, Arrays.asList(unfollowedUserItem));

        ToggleButton followButton = getFollowToggleButton();
        expect(followButton.isChecked()).toEqual(false);
        expect(followButton.getVisibility()).toEqual(View.VISIBLE);
    }

    private ToggleButton getFollowToggleButton() {
        return ((ToggleButton) itemView.findViewById(R.id.toggle_btn_follow));
    }
}
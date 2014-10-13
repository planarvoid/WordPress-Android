package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class UserItemPresenterTest {

    @InjectMocks private UserItemPresenter presenter;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private UserItemPresenter.OnToggleFollowListener toggleFollowListener;
    @Mock private FeatureFlags featureFlags;

    private View itemView;
    private PropertySet propertySet;

    @Before
    public void setup() {
        when(featureFlags.isEnabled(Feature.API_MOBILE_SEARCH)).thenReturn(false);
        propertySet = PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(2)),
                UserProperty.USERNAME.bind("forss"),
                UserProperty.COUNTRY.bind("Germany"),
                UserProperty.FOLLOWERS_COUNT.bind(42)
        );

        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.user_list_item, new FrameLayout(context), false);
    }

    @Test
    public void shouldBindUsernameToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_header).getText()).toEqual("forss");
    }

    @Test
    public void shouldBindCountryToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("Germany");
    }

    @Test
    public void shouldBindFollowersCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getText()).toEqual("42");
    }

    @Test
    public void shouldNotBindFollowersCountToViewIfNotSet() {
        propertySet.put(UserProperty.FOLLOWERS_COUNT, Consts.NOT_SET);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldFallBackToEmptyStringIfUserCountryNotSet() {
        final PropertySet homelessUser = PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(2)),
                UserProperty.USERNAME.bind("forss"),
                UserProperty.FOLLOWERS_COUNT.bind(42)
        );
        presenter.bindItemView(0, itemView, Arrays.asList(homelessUser));
        expect(textView(R.id.list_item_subheader).getText()).toEqual("");
    }

    @Test
    public void shouldLoadUserImage() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        verify(imageOperations).displayInAdapterView(
                Urn.forUser(2),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    @Test
    public void followButtonShouldBeHiddenWhenLookingAtOwnUserCell() {
        when(accountOperations.isLoggedInUser(any(Urn.class))).thenReturn(true);

        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(followingButton()).toBeGone();
    }

    @Test
    public void followButtonShouldBeHiddenWhenApiSearchFeatureFlagIsOn() {
        when(featureFlags.isEnabled(Feature.API_MOBILE_SEARCH)).thenReturn(true);

        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(followingButton()).toBeGone();
    }

    @Test
    public void followButtonShouldBeCheckedIfUserIsFollowed() {
        propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, true);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(followingButton()).toBeVisible();
        expect(followingButton()).toBeChecked();
    }
    
    @Test
    public void followButtonShouldBeUncheckedIfUserIsNotFollowed() {
        propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, false);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(followingButton()).toBeVisible();
        expect(followingButton()).not.toBeChecked();
    }

    @Test
    public void shouldForwardClicksToListenerWhenClickingFollowButton() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        presenter.setToggleFollowListener(toggleFollowListener);
        followingButton().performClick();
        verify(toggleFollowListener).onToggleFollowClicked(0, followingButton());
    }

    @Test
    public void shouldNotForwardClicksWhenClickingFollowButtonAndNoListenerSet() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        followingButton().performClick();
        verifyZeroInteractions(toggleFollowListener);
    }

    private ToggleButton followingButton() {
        return (ToggleButton) itemView.findViewById(R.id.toggle_btn_follow);
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

@RunWith(SoundCloudTestRunner.class)
public class UserItemRendererTest {

    private UserItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, Robolectric.application.getResources());

    private View itemView;
    private PropertySet propertySet;
    private UserItem userItem;

    @Before
    public void setup() {
        renderer = new UserItemRenderer(imageOperations, numberFormatter);

        propertySet = PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(2)),
                UserProperty.USERNAME.bind("forss"),
                UserProperty.COUNTRY.bind("Germany"),
                UserProperty.FOLLOWERS_COUNT.bind(42)
        );
        userItem = UserItem.from(propertySet);

        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.user_list_item, new FrameLayout(context), false);
    }

    @Test
    public void shouldBindUsernameToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        expect(textView(R.id.list_item_header).getText()).toEqual("forss");
    }

    @Test
    public void shouldBindCountryToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("Germany");
    }

    @Test
    public void shouldBindFollowersCountToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        expect(textView(R.id.list_item_counter).getText()).toEqual("42");
    }

    @Test
    public void shouldNotBindFollowersCountToViewIfNotSet() {
        propertySet.put(UserProperty.FOLLOWERS_COUNT, Consts.NOT_SET);
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldFallBackToEmptyStringIfUserCountryNotSet() {
        final UserItem homelessUser = UserItem.from(PropertySet.from(
                UserProperty.URN.bind(Urn.forUser(2)),
                UserProperty.USERNAME.bind("forss"),
                UserProperty.FOLLOWERS_COUNT.bind(42)
        ));
        renderer.bindItemView(0, itemView, Arrays.asList(homelessUser));
        expect(textView(R.id.list_item_subheader).getText()).toEqual("");
    }

    @Test
    public void shouldLoadUserImage() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));
        verify(imageOperations).displayInAdapterView(
                Urn.forUser(2),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
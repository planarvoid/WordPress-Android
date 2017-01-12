package com.soundcloud.android.view.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

public class UserItemRendererTest extends AndroidUnitTest {

    public static final String USERNAME = "forss";
    public static final String COUNTRY = "Germany";
    public static final Urn USER_URN = Urn.forUser(2);
    public static final int FOLLOWERS_COUNT = 42;
    private UserItemRenderer renderer;

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;

    private final CondensedNumberFormatter numberFormatter =
            CondensedNumberFormatter.create(Locale.US, resources());

    private View itemView;
    private UserItem userItem;

    @Before
    public void setup() {
        renderer = new UserItemRenderer(imageOperations, numberFormatter);

        userItem = UserItem.create(USER_URN, USERNAME, Optional.absent(), Optional.of(COUNTRY), FOLLOWERS_COUNT, false);

        itemView = LayoutInflater.from(context()).inflate(R.layout.user_list_item, new FrameLayout(context()), false);
    }

    @Test
    public void shouldBindUsernameToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        assertThat(textView(R.id.list_item_header).getText()).isEqualTo(USERNAME);
    }

    @Test
    public void shouldBindCountryToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        assertThat(textView(R.id.list_item_subheader).getText()).isEqualTo(COUNTRY);
    }

    @Test
    public void shouldBindFollowersCountToView() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));

        assertThat(textView(R.id.list_item_counter).getText()).isEqualTo("42");
    }

    @Test
    public void shouldNotBindFollowersCountToViewIfNotSet() {
        final UserItem userItemWithNoFollowers = UserItem.create(USER_URN, USERNAME, Optional.absent(), Optional.of(COUNTRY), Consts.NOT_SET, false);
        renderer.bindItemView(0, itemView, Arrays.asList(userItemWithNoFollowers));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldFallBackToEmptyStringIfUserCountryNotSet() {
        final UserItem homelessUser = UserItem.create(USER_URN, USERNAME, Optional.absent(), Optional.absent(), FOLLOWERS_COUNT, false);
        renderer.bindItemView(0, itemView, Arrays.asList(homelessUser));
        assertThat(textView(R.id.list_item_subheader).getText()).isEqualTo("");
    }

    @Test
    public void shouldLoadUserImage() {
        renderer.bindItemView(0, itemView, Arrays.asList(userItem));
        verify(imageOperations).displayCircularInAdapterView(
                userItem,
                ApiImageSize.getListItemImageSize(itemView.getResources()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}

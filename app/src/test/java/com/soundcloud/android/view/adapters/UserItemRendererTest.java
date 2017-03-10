package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.userBuilder;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.userItem;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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

import java.util.Locale;

public class UserItemRendererTest extends AndroidUnitTest {

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

        userItem = ModelFixtures.userItem();

        itemView = LayoutInflater.from(context()).inflate(R.layout.user_list_item, new FrameLayout(context()), false);
    }

    @Test
    public void shouldBindUsernameToView() {
        renderer.bindItemView(0, itemView, singletonList(userItem));

        assertThat(textView(R.id.list_item_header).getText()).isEqualTo(userItem.name());
    }

    @Test
    public void shouldBindCountryToView() {
        renderer.bindItemView(0, itemView, singletonList(userItem));

        assertThat(textView(R.id.list_item_subheader).getText()).isEqualTo(userItem.country().get());
    }

    @Test
    public void shouldBindFollowersCountToView() {
        renderer.bindItemView(0, itemView, singletonList(userItem));

        assertThat(textView(R.id.list_item_counter).getText()).isEqualTo(String.valueOf(userItem.followersCount()));
    }

    @Test
    public void shouldNotBindFollowersCountToViewIfNotSet() {
        final UserItem userItemWithNoFollowers = userItem(userBuilder().followersCount(Consts.NOT_SET).build());
        renderer.bindItemView(0, itemView, singletonList(userItemWithNoFollowers));

        assertThat(textView(R.id.list_item_counter).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldFallBackToEmptyStringIfUserCountryNotSet() {
        final UserItem homelessUser = userItem(userBuilder().country(Optional.absent()).build());
        renderer.bindItemView(0, itemView, singletonList(homelessUser));
        assertThat(textView(R.id.list_item_subheader).getText()).isEqualTo("");
    }

    @Test
    public void shouldLoadUserImage() {
        renderer.bindItemView(0, itemView, singletonList(userItem));
        verify(imageOperations).displayCircularInAdapterView(
                userItem,
                ApiImageSize.getListItemImageSize(itemView.getResources()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}

package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistItemPresenterTest {

    private PlaylistItemPresenter presenter;

    private View itemView;

    private PropertySet propertySet;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                PlayableProperty.URN.bind(Urn.forPlaylist(123)),
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR.bind("creator"),
                PlayableProperty.LIKES_COUNT.bind(5),
                PlayableProperty.IS_LIKED.bind(false),
                PlaylistProperty.TRACK_COUNT.bind(11)
        );

        final Context context = Robolectric.application;
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        itemView = layoutInflater.inflate(R.layout.playlist_list_item, new FrameLayout(context), false);

        presenter = new PlaylistItemPresenter(layoutInflater, context.getResources());
    }

    @Test
    public void shouldBindTitleToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.title).getText()).toEqual("title");
    }

    @Test
    public void shouldBindCreatorToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.username).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindTrackCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.track_count).getText()).toEqual("11 tracks");
    }

    @Test
    public void shouldBindLikesCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.likes_count).getText()).toEqual("5");
    }

    @Test
    public void shouldBindLikeStatusToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        expect(textView(R.id.likes_count).getCompoundDrawables()[0].getLevel()).toEqual(0);

        propertySet.put(PlayableProperty.IS_LIKED, true);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        expect(textView(R.id.likes_count).getCompoundDrawables()[0].getLevel()).toEqual(1);
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(PlayableProperty.REPOSTER, "reposter");
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.VISIBLE);
        expect(textView(R.id.reposter).getText()).toEqual("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.reposter).getVisibility()).toBe(View.GONE);
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class TrackItemPresenterTest {

    @InjectMocks
    private TrackItemPresenter presenter;

    @Mock
    private LayoutInflater inflater;
    @Mock
    private ImageOperations imageOperations;

    private View itemView;

    private PropertySet propertySet;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR_NAME.bind("creator"),
                PlayableProperty.DURATION.bind(227000),
                PlayableProperty.URN.bind(Urn.forTrack(123)),
                TrackProperty.PLAY_COUNT.bind(870)
        );

        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.track_list_item, new FrameLayout(context), false);
    }

    @Test
    public void shouldBindTitleToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("title");
    }

    @Test
    public void shouldBindDurationToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_right_info).getText()).toEqual("3:47");
    }

    @Test
    public void shouldBindCreatorToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_header).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindPlayCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getText()).toEqual("870");
    }

    @Test
    public void shouldHidePlayCountIfPlayCountNotSet() {
        propertySet.put(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.INVISIBLE);
    }

    @Test
    public void shouldHidePlayCountIfEqualOrLessZero() {
        propertySet.put(TrackProperty.PLAY_COUNT, 0);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter)).toBeInvisible();
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

    @Test
    public void shouldShowPrivateIndicatorIfTrackIsPrivate() {
        propertySet.put(PlayableProperty.IS_PRIVATE, true);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.now_playing).getVisibility()).toEqual(View.INVISIBLE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.INVISIBLE);
    }

    @Test
    public void shouldHidePrivateIndicatorIfTrackIsPublic() {
        propertySet.put(PlayableProperty.IS_PRIVATE, false);
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.private_indicator).getVisibility()).toEqual(View.GONE);
        expect(textView(R.id.now_playing).getVisibility()).toEqual(View.INVISIBLE);
        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldHighlightCurrentlyPlayingTrack() {
        presenter.setPlayingTrack(Urn.forTrack(123));
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.INVISIBLE);
        expect(textView(R.id.now_playing).getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldNotHighlightTrackRowIfNotCurrentlyPlayingTrack() {
        presenter.setPlayingTrack(Urn.forTrack(-1));
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.list_item_counter).getVisibility()).toEqual(View.VISIBLE);
        expect(textView(R.id.now_playing).getVisibility()).toEqual(View.INVISIBLE);
    }

    @Test
    public void shouldLoadTrackArtwork() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));
        verify(imageOperations).displayInAdapterView(
                Urn.forTrack(123),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}
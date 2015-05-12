package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
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

    @Mock private LayoutInflater inflater;
    @Mock private ImageOperations imageOperations;
    @Mock private EventBus eventBus;
    @Mock private ScreenProvider screenProvider;

    private View itemView;
    private PropertySet propertySet;
    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                TrackProperty.TITLE.bind("title"),
                TrackProperty.CREATOR_NAME.bind("creator"),
                TrackProperty.DURATION.bind(227000),
                TrackProperty.URN.bind(Urn.forTrack(123)),
                TrackProperty.PLAY_COUNT.bind(870)
        );
        trackItem = TrackItem.from(propertySet);

        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.track_list_item, new FrameLayout(context), false);
    }

    @Test
    public void shouldBindTitleToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_subheader).getText()).toEqual("title");
    }

    @Test
    public void shouldBindDurationToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_right_info).getText()).toEqual("3:47");
    }

    @Test
    public void shouldBindCreatorToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_header).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindPlayCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_counter).getText()).toEqual("870");
    }

    @Test
    public void shouldHidePlayCountIfPlayCountNotSet() {
        propertySet.put(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_counter)).toBeInvisible();
    }

    @Test
    public void shouldHidePlayCountIfEqualOrLessZero() {
        propertySet.put(TrackProperty.PLAY_COUNT, 0);
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_counter)).toBeInvisible();
    }

    @Test
    public void shouldBindReposterIfAny() {
        propertySet.put(TrackProperty.REPOSTER, "reposter");
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.reposter)).toBeVisible();
        expect(textView(R.id.reposter).getText()).toEqual("reposter");
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.reposter)).toBeGone();
    }

    @Test
    public void shouldShowPrivateIndicatorIfTrackIsPrivate() {
        propertySet.put(TrackProperty.IS_PRIVATE, true);
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.private_indicator)).toBeVisible();
        expect(textView(R.id.now_playing)).toBeInvisible();
        expect(textView(R.id.list_item_counter)).toBeInvisible();
    }

    @Test
    public void shouldHidePrivateIndicatorIfTrackIsPublic() {
        propertySet.put(TrackProperty.IS_PRIVATE, false);
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.private_indicator)).toBeGone();
        expect(textView(R.id.now_playing)).toBeInvisible();
        expect(textView(R.id.list_item_counter)).toBeVisible();
    }

    @Test
    public void shouldHighlightCurrentlyPlayingTrack() {
        presenter.setPlayingTrack(Urn.forTrack(123));
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_counter)).toBeInvisible();
        expect(textView(R.id.now_playing)).toBeVisible();
    }

    @Test
    public void shouldNotHighlightTrackRowIfNotCurrentlyPlayingTrack() {
        presenter.setPlayingTrack(Urn.forTrack(-1));
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));

        expect(textView(R.id.list_item_counter)).toBeVisible();
        expect(textView(R.id.now_playing)).toBeInvisible();
    }

    @Test
    public void shouldLoadTrackArtwork() {
        presenter.bindItemView(0, itemView, Arrays.asList(trackItem));
        verify(imageOperations).displayInAdapterView(
                Urn.forTrack(123),
                ApiImageSize.getListItemImageSize(itemView.getContext()),
                (android.widget.ImageView) itemView.findViewById(R.id.image));
    }

    @Test
    public void shouldShowPromotedIndicator() {
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        presenter.bindItemView(0, itemView, Arrays.asList(promotedTrackItem));

        expect(textView(R.id.promoted_track)).toBeVisible();
        expect(textView(R.id.promoted_track)).toHaveText("Promoted");
    }

    @Test
    public void shouldShowPromotedIndicatorWithPromoter() {
        TrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        presenter.bindItemView(0, itemView, Arrays.asList(promotedTrackItem));

        expect(textView(R.id.promoted_track)).toBeVisible();
        expect(textView(R.id.promoted_track)).toHaveText("Promoted by SoundCloud");
    }

    @Test
    public void clickingOnPromotedIndicatorFiresTrackingEvent() {
        when(screenProvider.getLastScreenTag()).thenReturn("stream");
        PromotedTrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        presenter.bindItemView(0, itemView, Arrays.asList((TrackItem) promotedTrackItem));

        itemView.findViewById(R.id.promoted_track).performClick();

        verify(eventBus).publish(eq(EventQueue.TRACKING), any(PromotedTrackEvent.class));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }

}

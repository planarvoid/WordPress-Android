package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    private View itemView;

    private PropertySet propertySet;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                PlayableProperty.TITLE.bind("title"),
                PlayableProperty.CREATOR.bind("creator"),
                PlayableProperty.DURATION.bind(227000),
                PlayableProperty.URN.bind(Urn.forTrack(0)),
                TrackProperty.PLAY_COUNT.bind(870)
        );

        final Context context = Robolectric.application;
        itemView = LayoutInflater.from(context).inflate(R.layout.track_list_item, new FrameLayout(context), false);
    }

    @Test
    public void shouldBindTitleToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.title).getText()).toEqual("title");
    }

    @Test
    public void shouldBindDurationToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.duration).getText()).toEqual("3.47");
    }

    @Test
    public void shouldBindCreatorToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.username).getText()).toEqual("creator");
    }

    @Test
    public void shouldBindPlayCountToView() {
        presenter.bindItemView(0, itemView, Arrays.asList(propertySet));

        expect(textView(R.id.play_count).getText()).toEqual("870");
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
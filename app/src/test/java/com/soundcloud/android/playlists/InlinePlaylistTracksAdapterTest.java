package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class InlinePlaylistTracksAdapterTest {

    private InlinePlaylistTracksAdapter adapter;

    @Mock
    private InlinePlaylistTrackPresenter presenter;

    @Before
    public void setUp() throws Exception {
        adapter = new InlinePlaylistTracksAdapter(presenter);
    }

    @Test
    public void reports2DifferentItemTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void hasCountOf1WithNoDataAndInlineEmptyViews() throws Exception {
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void hasContentItemsShouldBeFalseWhenNoItemsHaveBeenSet() {
        expect(adapter.hasContentItems()).toBeFalse();
    }

    @Test
    public void hasContentItemsShouldBeTrueOnceItemsHaveBeenAdded() {
        adapter.addItem(new Track(1));
        expect(adapter.hasContentItems()).toBeTrue();
    }
}

package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.LegacyPlayableRowPresenter;
import com.soundcloud.android.view.adapters.LegacyUserRowPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsAdapterTest {

    private SearchResultsAdapter adapter;

    @Mock
    private LegacyPlayableRowPresenter playablePresenter;
    @Mock
    private LegacyUserRowPresenter userPresenter;

    @Before
    public void setup() {
        adapter = new SearchResultsAdapter(userPresenter, playablePresenter);
    }

    @Test
    public void shouldDifferentiateItemViewTypes() {
        adapter.addItem(new User());
        adapter.addItem(new Track());

        expect(adapter.getItemViewType(0)).toEqual(SearchResultsAdapter.TYPE_USER);
        expect(adapter.getItemViewType(1)).toEqual(SearchResultsAdapter.TYPE_PLAYABLE);
    }

}

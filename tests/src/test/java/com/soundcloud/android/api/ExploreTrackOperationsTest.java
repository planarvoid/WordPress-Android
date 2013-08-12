package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTrackOperationsTest {

    private ExploreTrackOperations exploreTrackOperations;

    @Before
    public void setUp() {
        exploreTrackOperations = new ExploreTrackOperations();
    }

    @Test
    public void shouldLoadCategories(){
        Observable<ExploreTracksCategories> categoriesObservable = exploreTrackOperations.getCategories();
        ExploreTracksCategories categories = categoriesObservable.toBlockingObservable().last();
        expect(categories.getMusic().size()).toEqual(12);
        expect(categories.getAudio().get(0).getKey()).toEqual("popular_audio");
        expect(categories.getAudio().get(0).getLinks().get("suggested_tracks").getHref()).toEqual("http://localhost:9090/suggestions/tracks/categories/popular_audio");
    }
}

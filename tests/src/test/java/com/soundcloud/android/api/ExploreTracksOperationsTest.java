package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksOperationsTest {

    private ExploreTracksOperations exploreTracksOperations;

    @Before
    public void setUp() {
        exploreTracksOperations = new ExploreTracksOperations();
    }

    @Test
    @Ignore
    public void shouldLoadCategories(){
        Observable<ExploreTracksCategory> categoriesObservable = exploreTracksOperations.getCategories();
        ArrayList<ExploreTracksCategory> categories = Lists.newArrayList(categoriesObservable.toBlockingObservable().toIterable());

        expect(categories.size()).toEqual(24);
        expect(categories.get(0).getTitle()).toEqual("popular_music");
        expect(categories.get(0).getLinks().get("suggested_tracks").getHref()).toEqual("http://localhost:9090/suggestions/tracks/categories/popular_music");
    }

    @Test
    @Ignore
    public void shouldSetCategorySectionTitles(){
        Observable<ExploreTracksCategory> categoriesObservable = exploreTracksOperations.getCategories();
        for (ExploreTracksCategory category : categoriesObservable.toBlockingObservable().toIterable()){
            expect(category.getSection()).not.toBeNull();
        }

    }
}

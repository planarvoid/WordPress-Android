package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoryTest {

    private ExploreTracksCategory mCategory;

    @Test
    public void shouldBeParcelable() {
        mCategory = new ExploreTracksCategory("Title1");

        final Map<String, Link> links = new HashMap<String, Link>();
        links.put("link1", new Link("http://link1"));
        links.put("link2", new Link("http://link2"));
        mCategory.setLinks(links);

        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        ExploreTracksCategory category = new ExploreTracksCategory(parcel);
        expect(category.getTitle()).toEqual(mCategory.getTitle());
        expect(category.getLinks().get("link1")).toEqual(mCategory.getLinks().get("link1"));
        expect(category.getLinks().get("link2")).toEqual(mCategory.getLinks().get("link2"));

    }
}

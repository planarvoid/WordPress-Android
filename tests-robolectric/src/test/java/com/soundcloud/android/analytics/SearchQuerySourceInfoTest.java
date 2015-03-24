package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SearchQuerySourceInfoTest {

    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Test
    public void shouldGetPositionFromClickPosition() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, Urn.forPlaylist(321L));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forUser(789L))));

        expect(searchQuerySourceInfo.getUpdatedResultPosition(Urn.forTrack(123L))).toEqual(5);
    }

    @Test
    public void shouldGetPositionFromPlayPosition() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, Urn.forTrack(123L));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forUser(789L))));

        expect(searchQuerySourceInfo.getUpdatedResultPosition(Urn.forTrack(456L))).toEqual(1);
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, new Urn("some:click:123"));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forUser(789L))));

        Parcel parcel = Parcel.obtain();
        searchQuerySourceInfo.writeToParcel(parcel, 0);

        SearchQuerySourceInfo copy = new SearchQuerySourceInfo(parcel);
        expect(copy.getClickPosition()).toEqual(searchQuerySourceInfo.getClickPosition());
        expect(copy.getQueryUrn()).toEqual(searchQuerySourceInfo.getQueryUrn());
        expect(copy.getClickUrn()).toEqual(searchQuerySourceInfo.getClickUrn());
        expect(copy.getQueryResults()).toEqual(searchQuerySourceInfo.getQueryResults());
    }

}

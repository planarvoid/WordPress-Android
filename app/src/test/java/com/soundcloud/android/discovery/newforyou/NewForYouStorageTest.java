package com.soundcloud.android.discovery.newforyou;

import static com.soundcloud.android.discovery.newforyou.NewForYouStorage.FILE_NAME;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.JsonFileStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.InsertResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NewForYouStorageTest extends AndroidUnitTest {

    public static final Urn QUERY_URN = Urn.forNewForYou("abc");
    @Mock TrackRepository trackRepository;
    @Mock StoreTracksCommand storeTracksCommand;
    @Mock JsonFileStorage fileStorage;

    private NewForYouStorage storage;

    private final ApiTrack firstTrack = ModelFixtures.create(ApiTrack.class);
    private final ApiTrack secondTrack = ModelFixtures.create(ApiTrack.class);
    private final List<ApiTrack> trackList = Lists.newArrayList(firstTrack, secondTrack);
    private final ModelCollection<ApiTrack> trackModelCollection = new ModelCollection<>(trackList, Collections.emptyMap(), QUERY_URN);

    private final ApiNewForYou apiNewForYou = ApiNewForYou.create(new Date(), trackModelCollection);

    @Before
    public void setUp() throws Exception {
        when(storeTracksCommand.call(any())).thenReturn(new InsertResult(1));

        storage = new NewForYouStorage(trackRepository, storeTracksCommand, fileStorage);
    }

    @Test
    public void storeNewForYou() throws Exception {
        storage.storeNewForYou(apiNewForYou);
        verify(storeTracksCommand).call(trackModelCollection);
        verify(fileStorage).writeToFile(FILE_NAME, NewForYouStorage.NewForYouStorageItem.fromApiNewForYou(apiNewForYou));
    }

    @Test
    public void loadsNewForYou() throws Exception {
        List<Track> trackList = Collections.singletonList(Track.from(firstTrack));
        when(trackRepository.trackListFromUrns(asList(firstTrack.getUrn(), secondTrack.getUrn()))).thenReturn(Observable.just(trackList));
        when(fileStorage.readFromFile(eq(FILE_NAME), any())).thenReturn(Observable.just(NewForYouStorage.NewForYouStorageItem.fromApiNewForYou(apiNewForYou)));

        storage.newForYou().test().assertValue(NewForYou.create(apiNewForYou.lastUpdate(), QUERY_URN, trackList));
    }
}

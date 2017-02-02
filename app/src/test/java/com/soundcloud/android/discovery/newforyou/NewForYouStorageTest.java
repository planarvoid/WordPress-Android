package com.soundcloud.android.discovery.newforyou;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.JsonFileStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.InsertResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NewForYouStorageTest extends AndroidUnitTest {

    @Mock TrackRepository trackRepository;
    @Mock StoreTracksCommand storeTracksCommand;
    @Mock JsonFileStorage fileStorage;

    private NewForYouStorage storage;

    @Before
    public void setUp() throws Exception {
        when(storeTracksCommand.call(any())).thenReturn(new InsertResult(1));

        storage = new NewForYouStorage(trackRepository, storeTracksCommand, fileStorage);
    }

    @Test
    public void storeNewForYou() throws Exception {
        ApiTrack firstTrack = ModelFixtures.create(ApiTrack.class);
        ApiTrack secondTrack = ModelFixtures.create(ApiTrack.class);
        List<ApiTrack> trackList = Lists.newArrayList(firstTrack, secondTrack);
        ModelCollection<ApiTrack> trackModelCollection = new ModelCollection<>(trackList, Collections.emptyMap(), Urn.forNewForYou("abc"));

        ApiNewForYou apiNewForYou = ApiNewForYou.create(new Date(), trackModelCollection);

        storage.storeNewForYou(apiNewForYou);
        verify(storeTracksCommand).call(trackModelCollection);
        verify(fileStorage).writeToFile(anyString(), any());
    }
}

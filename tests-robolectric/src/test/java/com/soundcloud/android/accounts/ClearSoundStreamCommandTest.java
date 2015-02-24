package com.soundcloud.android.accounts;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ApiStreamItemFixtures;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ClearSoundStreamCommandTest extends StorageIntegrationTest {
    private ClearSoundStreamCommand clearSoundStreamCommand;

    @Before
    public void setup() {
        clearSoundStreamCommand = new ClearSoundStreamCommand(propeller());
    }

    @Test
    public void shouldClearSoundStreamItems() throws PropellerWriteException {
        testFixtures().insertStreamTrackPost(ApiStreamItemFixtures.trackPost());
        expectStreamItemCountToBe(1);

        clearSoundStreamCommand.call();
        expectStreamItemCountToBe(0);
    }

    protected void expectStreamItemCountToBe(int count){
        assertThat(select(from(Table.SoundStream.name())), counts(count));
    }

}
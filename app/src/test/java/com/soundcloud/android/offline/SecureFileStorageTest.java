package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.google.common.base.Charsets;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(SoundCloudTestRunner.class)
public class SecureFileStorageTest {

    private SecureFileStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new SecureFileStorage();
    }

    @Test
    public void offlineTracksDirectoryIsCreated() {
        expect(storage.OFFLINE_DIR.exists()).toBeTrue();
    }

    @Test
    public void storeTrackSavesDataToAFile() throws IOException {
        String expectedContent = "someContent";
        File file = new File(storage.OFFLINE_DIR, "track123.mp3");
        InputStream someData = new ByteArrayInputStream(expectedContent.getBytes(Charsets.UTF_8));

        storage.storeTrack(Urn.forTrack(123), someData);

        expect(file.exists()).toBeTrue();
        expect(readFileContent(file)).toEqual(expectedContent);
    }

    private String readFileContent(File file) throws IOException {
        return IOUtils.readInputStream(new FileInputStream(file));
    }
}
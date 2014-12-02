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
    private final String testContent = "content";

    @Before
    public void setUp() throws Exception {
        storage = new SecureFileStorage();
    }

    @Test
    public void offlineTracksDirectoryIsCreated() {
        expect(storage.OFFLINE_DIR.exists()).toBeTrue();
    }

    @Test
    public void offlineTrackDirectoryIsReusedWhenAlreadyExists() throws IOException {
        File file = new File(storage.OFFLINE_DIR, "track124.mp3");
        SecureFileStorage otherStorage = new SecureFileStorage();

        otherStorage.storeTrack(Urn.forTrack(124), sampleDataStream());

        expect(file.exists()).toBeTrue();
    }

    @Test
    public void storeTrackSavesDataToAFile() throws IOException {
        File file = new File(storage.OFFLINE_DIR, "track123.mp3");
        InputStream someData = sampleDataStream();

        storage.storeTrack(Urn.forTrack(123), someData);

        expect(file.exists()).toBeTrue();
        expect(readFileContent(file)).toEqual(testContent);
    }

    private String readFileContent(File file) throws IOException {
        return IOUtils.readInputStream(new FileInputStream(file));
    }

    private InputStream sampleDataStream() {
        return new ByteArrayInputStream(testContent.getBytes(Charsets.UTF_8));
    }
}
package com.soundcloud.android.streaming;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.BufferUtils;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.shadows.ShadowStatFs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static android.content.SharedPreferences.Editor;
import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;

@RunWith(DefaultTestRunner.class)
public class StreamStorageTest {
    public static final int TEST_CHUNK_SIZE = 1024;
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test-"+(int)(Math.random()*10E6));
    final File testFile = new File(getClass().getResource("fred.mp3").getFile());
    final LinkedHashMap<Integer, ByteBuffer> sampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    final List<Integer> sampleChunkIndexes = new ArrayList<Integer>();

    private StreamStorage storage;
    private StreamItem item;
    private ApplicationProperties applicationProperties;


    @Before
    public void before() {
        IOUtils.deleteDir(baseDir);
        applicationProperties = mock(ApplicationProperties.class);
        storage = new StreamStorage(DefaultTestRunner.application, baseDir,applicationProperties, TEST_CHUNK_SIZE, 0);
        item = new StreamItem("https://api.soundcloud.com/tracks/1234/stream", testFile);
        TestHelper.enableSDCard();
    }

    private int setupChunkArray() throws IOException {
        FileChannel fc = new FileInputStream(testFile).getChannel();
        int chunks = 0;
        ByteBuffer buffer = ByteBuffer.allocate(storage.chunkSize);
        while (fc.read(buffer) != -1) {
            sampleBuffers.put(chunks, BufferUtils.clone(buffer));
            sampleChunkIndexes.add(chunks);
            chunks++;
        }
        return chunks;
    }

    @Test
    public void testSetDataShouldNotStoreIfContentLengthZero() throws IOException {
        expect(storage.storeData(item.getUrl(), ByteBuffer.allocate(0), 0)).toBeFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataShouldNotStoreIfDataNull() throws IOException {
        storage.storeData(item.getUrl(), null, 0);
    }

    @Test
    public void shouldStoreData() throws Exception {
        expect(storage.storeMetadata(item)).toBeTrue();
        expect(storage.storeData(item.getUrl(), ByteBuffer.wrap(new byte[]{1, 2, 3}), 0)).toBeTrue();
        ByteBuffer data = storage.getChunkData(item.getUrl(), 0);
        expect(data).not.toBeNull();
        expect(data.limit()).toEqual(storage.chunkSize);
        expect(data.get()).toEqual((byte) 1);
        expect(data.get()).toEqual((byte) 2);
        expect(data.get()).toEqual((byte) 3);
    }

    @Test
    public void shouldNotStoreDataWhenSDCardNotAvailable() throws Exception {
        TestHelper.disableSDCard();
        expect(storage.storeMetadata(item)).toBeTrue();
        expect(storage.storeData(item.getUrl(), ByteBuffer.wrap(new byte[]{1, 2, 3}), 0)).toBeFalse();
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        ShadowStatFs.registerStats(baseDir, 100, 100, 100);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(DefaultTestRunner.application);
        Editor editor = sharedPreferences.edit();

        editor.putInt(Settings.STREAM_CACHE_SIZE, 0);
        editor.commit();

        expect(storage.calculateUsableSpace()).toBe(0L);

        editor.putInt(Settings.STREAM_CACHE_SIZE, 33);
        editor.commit();

        expect(storage.calculateUsableSpace()).toEqual(ShadowStatFs.BLOCK_SIZE * 33L);

        editor.putInt(Settings.STREAM_CACHE_SIZE, 100);
        editor.commit();

        expect(storage.calculateUsableSpace()).toEqual(ShadowStatFs.BLOCK_SIZE * 100L);
    }

    @Test
    public void shouldTestIncompleteSequentialWriting() throws Exception {
        setupChunkArray();

        expect(storage.storeMetadata(item)).toBeTrue();
        final int writing = sampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(item.getUrl(), sampleBuffers.get(i), i);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item.getUrl(), i)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestIncompleteRandomWriting() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        final int writing = sampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(item.getUrl(), sampleBuffers.get(sampleChunkIndexes.get(i)), sampleChunkIndexes.get(i));
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item.getUrl(), sampleChunkIndexes.get(i))).not.toBeNull();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionIfChunkIsNotAvailable() throws Exception {
        storage.getChunkData(item.getUrl(), 0);
    }


    @Test
    public void shouldTestCompleteSequentialWriting() throws Exception {
        setupChunkArray();

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i = 0; i < sampleChunkIndexes.size(); i++) {
            storage.storeData(item.getUrl(), sampleBuffers.get(i), i);
        }

        for (int i = 0; i < sampleChunkIndexes.size(); i++) {
            expect(storage.getChunkData(item.getUrl(), i)).not.toBeNull();
        }

        try {
            storage.getChunkData(item.getUrl(), sampleChunkIndexes.size());
            fail("expected IO exception");
        } catch (IOException e) { /* expected */ }
    }

    @Test
    public void shouldTestCompleteRandomWriting() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(item.getUrl(), sampleBuffers.get(i), i);
        }

        for (int i : sampleChunkIndexes) {
            expect(storage.getChunkData(item.getUrl(), i)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestCompleteFileConstruction() throws Exception {
        int chunks = setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(item.getUrl(), sampleBuffers.get(i), i);
        }

        expect(item.numberOfChunks(storage.chunkSize)).toBe(chunks);

        File assembled = storage.completeFileForUrl(item.streamItemUrl());
        expect(assembled.exists()).toBeTrue();
        expect(assembled.length()).toEqual(item.getContentLength());

        // make sure index file is gone
        expect(storage.incompleteFileForUrl(item.streamItemUrl()).exists()).toBeFalse();
        String original = IOUtils.md5(getClass().getResourceAsStream("fred.mp3"));
        expect(IOUtils.md5(new FileInputStream(assembled))).toEqual(original);
    }

    @Test
    public void shouldCheckEtagOfCompleteItem() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        StreamItem wrongEtag = new StreamItem(item.streamItemUrl(), item.getContentLength(), "deadbeef");

        expect(storage.storeMetadata(wrongEtag)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(wrongEtag.getUrl(), sampleBuffers.get(i), i);
        }
        File assembled = storage.completeFileForUrl(wrongEtag.streamItemUrl());
        expect(assembled.exists()).toBeFalse();
    }

    @Test
    public void shouldReturnMissingIndexes() throws Exception {
        Index index = storage.getMissingChunksForItem(item.streamItemUrl(), item.chunkRange(storage.chunkSize));
        expect(index.size()).toEqual(51);
    }
}

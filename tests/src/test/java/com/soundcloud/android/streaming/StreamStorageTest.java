package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.fail;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Environment;

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

@RunWith(DefaultTestRunner.class)
public class StreamStorageTest {
    public static final int TEST_CHUNK_SIZE = 1024;
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test-"+(int)(Math.random()*10E6));
    final File testFile = new File(getClass().getResource("fred.mp3").getFile());
    final LinkedHashMap<Integer, ByteBuffer> sampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    final List<Integer> sampleChunkIndexes = new ArrayList<Integer>();

    StreamStorage storage;
    StreamItem item;

    @Before
    public void before() {
        IOUtils.deleteDir(baseDir);
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, TEST_CHUNK_SIZE, 0);
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
        expect(storage.storeData(item.url, ByteBuffer.allocate(0), 0)).toBeFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataShouldNotStoreIfDataNull() throws IOException {
        storage.storeData(item.url, null, 0);
    }

    @Test
    public void shouldStoreData() throws Exception {
        expect(storage.storeMetadata(item)).toBeTrue();
        expect(storage.storeData(item.url, ByteBuffer.wrap(new byte[]{1, 2, 3}), 0)).toBeTrue();
        ByteBuffer data = storage.getChunkData(item.url, 0);
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
        expect(storage.storeData(item.url, ByteBuffer.wrap(new byte[]{1, 2, 3}), 0)).toBeFalse();
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        storage.calculateFileMetrics();
    }

    @Test
    public void shouldTestIncompleteSequentialWriting() throws Exception {
        setupChunkArray();

        expect(storage.storeMetadata(item)).toBeTrue();
        final int writing = sampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(item.url, sampleBuffers.get(i), i);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item.url, i)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestIncompleteRandomWriting() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        final int writing = sampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(item.url, sampleBuffers.get(sampleChunkIndexes.get(i)), sampleChunkIndexes.get(i));
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item.url, sampleChunkIndexes.get(i))).not.toBeNull();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionIfChunkIsNotAvailable() throws Exception {
       storage.getChunkData(item.url, 0);
    }


    @Test
    public void shouldTestCompleteSequentialWriting() throws Exception {
        setupChunkArray();

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i = 0; i < sampleChunkIndexes.size(); i++) {
            storage.storeData(item.url, sampleBuffers.get(i), i);
        }

        for (int i = 0; i < sampleChunkIndexes.size(); i++) {
            expect(storage.getChunkData(item.url, i)).not.toBeNull();
        }

        try {
            storage.getChunkData(item.url, sampleChunkIndexes.size());
            fail("expected IO exception");
        } catch (IOException e) { /* expected */ }
    }

    @Test
    public void shouldTestCompleteRandomWriting() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(item.url, sampleBuffers.get(i), i);
        }

        for (int i : sampleChunkIndexes) {
            expect(storage.getChunkData(item.url, i)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestCompleteFileConstruction() throws Exception {
        int chunks = setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        expect(storage.storeMetadata(item)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(item.url, sampleBuffers.get(i), i);
        }

        expect(item.numberOfChunks(storage.chunkSize)).toBe(chunks);

        File assembled = storage.completeFileForUrl(item.url.toString());
        expect(assembled.exists()).toBeTrue();
        expect(assembled.length()).toEqual(item.getContentLength());

        // make sure index file is gone
        expect(storage.incompleteFileForUrl(item.url.toString()).exists()).toBeFalse();
        String original = IOUtils.md5(getClass().getResourceAsStream("fred.mp3"));
        expect(IOUtils.md5(new FileInputStream(assembled))).toEqual(original);
    }

    @Test
    public void shouldCheckEtagOfCompleteItem() throws Exception {
        setupChunkArray();
        Collections.shuffle(sampleChunkIndexes);

        StreamItem wrongEtag = new StreamItem(item.url.toString(), item.getContentLength(), "deadbeef");

        expect(storage.storeMetadata(wrongEtag)).toBeTrue();
        for (int i : sampleChunkIndexes) {
            storage.storeData(wrongEtag.url, sampleBuffers.get(i), i);
        }
        File assembled = storage.completeFileForUrl(wrongEtag.url.toString());
        expect(assembled.exists()).toBeFalse();
    }

    @Test
    public void shouldReturnMissingIndexes() throws Exception {
        Index index = storage.getMissingChunksForItem(item.url.toString(), item.chunkRange(storage.chunkSize));
        expect(index.size()).toEqual(51);
    }
}

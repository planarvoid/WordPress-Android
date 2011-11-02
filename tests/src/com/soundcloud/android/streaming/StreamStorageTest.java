package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.fail;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
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

@RunWith(DefaultTestRunner.class)
public class StreamStorageTest {
    public static final int TEST_CHUNK_SIZE = 1024;
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");
    final File testFile = new File(getClass().getResource("fred.mp3").getFile());
    final long sampleContentLength = testFile.length();
    final LinkedHashMap<Integer, ByteBuffer> sampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    final List<Integer> sampleChunkIndexes = new ArrayList<Integer>();

    StreamStorage storage;
    StreamItem item;

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, TEST_CHUNK_SIZE, false);
        item = new StreamItem("fred.mp3", sampleContentLength, CloudUtils.md5(testFile));
    }

    private int setupChunkArray() throws IOException {
        FileChannel fc = new FileInputStream(testFile).getChannel();
        int chunks = 0;
        ByteBuffer buffer = storage.getBuffer();
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
        item.setContentLength(10);
        storage.storeData(item.url, null, 0);
    }

    @Test
    public void shouldSetData() throws Exception {
        item.setContentLength(storage.chunkSize * 2);
        expect(storage.storeMetadata(item)).toBeTrue();
        expect(storage.storeData(item.url, ByteBuffer.wrap(new byte[]{1, 2, 3}), 0)).toBeTrue();
        ByteBuffer data = storage.getChunkData(item.url, 0);
        expect(data).not.toBeNull();
        expect(data.limit()).toEqual(storage.chunkSize);
        expect(data.get()).toBe((byte) 1);
        expect(data.get()).toBe((byte) 2);
        expect(data.get()).toBe((byte) 3);
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        storage.calculateFileMetrics();
    }

    @Test
    public void shouldTestIncompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(sampleContentLength);

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
        item.setContentLength(sampleContentLength);
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

        File assembled = storage.completeFileForUrl(item.url);
        expect(assembled.exists()).toBeTrue();
        expect(assembled.length()).toEqual(sampleContentLength);

        // make sure index file is gone
        expect(storage.incompleteFileForUrl(item.url).exists()).toBeFalse();
        String original = CloudUtils.md5(getClass().getResourceAsStream("fred.mp3"));
        expect(CloudUtils.md5(new FileInputStream(assembled))).toEqual(original);
    }

    @Test
    public void shouldReturnMissingIndexes() throws Exception {
        item.setContentLength(TEST_CHUNK_SIZE * 2 + 5);
        Index index = storage.getMissingChunksForItem(item.url, item.chunkRange(storage.chunkSize));
        expect(index.size()).toEqual(3);
    }
}

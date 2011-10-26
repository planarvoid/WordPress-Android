package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

@RunWith(DefaultTestRunner.class)
public class StreamStorageTest {
    public static final int TEST_CHUNK_SIZE = 1024;
    StreamStorage storage;
    StreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, TEST_CHUNK_SIZE);
        item = new StreamItem("fred.mp3");
    }

    private long mSampleContentLength;
    private LinkedHashMap<Integer, byte[]> mSampleBuffers;
    private ArrayList<Integer> mSampleChunkIndexes;

    private long setupChunkArray() throws IOException {

        InputStream inputStream = getClass().getResourceAsStream("fred.mp3");
        mSampleContentLength = new File(getClass().getResource("fred.mp3").getFile()).length();

        long chunks = 0;
        mSampleBuffers = new LinkedHashMap<Integer, byte[]>();
        mSampleChunkIndexes = new ArrayList<Integer>();
        do {
            byte[] buffer = new byte[storage.chunkSize];
            if (inputStream.read(buffer) == -1) {
                break;
            } else {
                mSampleBuffers.put((int) chunks, buffer);
                mSampleChunkIndexes.add((int) chunks);
                chunks++;
            }
        } while (true);
        return chunks;
    }

    @Test
    public void testSetDataShouldNotStoreIfContentLengthZero() throws IOException {
        expect(item.getContentLength()).toBe(0l);
        expect(storage.setData(new byte[]{1, 2, 3}, 0, item)).toBeFalse();
    }

    @Test
    public void testSetDataShouldNotStoreIfDataNull() throws IOException {
        item.setContentLength(10);
        expect(storage.setData(null, 0, item)).toBeFalse();
    }

    @Test
    public void shouldWriteAndReadIndex() throws Exception {
        item.setContentLength(100);
        storage.writeIndex(item, Arrays.asList(1, 2, 3, 4));

        StreamStorage other = new StreamStorage(DefaultTestRunner.application, baseDir);

        expect(other.isMetaDataLoaded(item)).toBeFalse();

        other.readIndex(item);
        expect(other.getIncompleteIndexes().get(item)).toContainInOrder(1,2,3,4);
        expect(other.getIncompleteContentLengths().get(item)).toBe(100l);

        expect(other.isMetaDataLoaded(item)).toBeTrue();
    }


    @Test
    public void shouldSetData() throws Exception {
        item.setContentLength(storage.chunkSize * 2);

        expect(storage.setData(new byte[]{1, 2, 3}, 0, item)).toBeTrue();
        byte[] data = storage.getChunkData(item, 0);
        expect(data).not.toBeNull();
        expect(data.length).toEqual(storage.chunkSize);
        expect(data[0]).toBe((byte) 1);
        expect(data[1]).toBe((byte) 2);
        expect(data[2]).toBe((byte) 3);
    }

    @Test
    public void shouldSetContentLength() throws Exception {
        storage.setContentLength(item, 2 * storage.chunkSize);
        expect(storage.getIncompleteContentLengths().get(item)).toEqual(2l * storage.chunkSize);
        expect(storage.numberOfChunksForItem(item)).toEqual(2l);
    }

    @Test
    public void shouldCalculateFileMetrics() throws Exception {
        storage.calculateFileMetrics();
    }

    @Test
    public void shouldTestIncompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.setData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item, i)).not.toBeNull();
        }
        expect(storage.getChunkData(item, writing)).toBeNull();
    }

    @Test
    public void shouldTestIncompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.setData(mSampleBuffers.get(mSampleChunkIndexes.get(i)), mSampleChunkIndexes.get(i), item);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item, mSampleChunkIndexes.get(i))).not.toBeNull();
        }
        expect(storage.getChunkData(item, mSampleChunkIndexes.get(writing))).toBeNull();
    }

    @Test
    public void shouldTestCompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            storage.setData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            expect(storage.getChunkData(item, i)).not.toBeNull();
        }

        expect(storage.getChunkData(item, mSampleChunkIndexes.size())).toBeNull();
    }

    @Test
    public void shouldTestCompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer mSampleChunkIndexe1 : mSampleChunkIndexes) {
            storage.setData(mSampleBuffers.get(mSampleChunkIndexe1), mSampleChunkIndexe1, item);
        }

        for (Integer mSampleChunkIndexe : mSampleChunkIndexes) {
            expect(storage.getChunkData(item, mSampleChunkIndexe)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestCompleteFileConstruction() throws Exception {
        long chunks = setupChunkArray();

        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer aChunkArray : mSampleChunkIndexes) {
            storage.setData(mSampleBuffers.get(aChunkArray), aChunkArray, item);
        }

        expect(storage.numberOfChunksForItem(item)).toBe(chunks);

        File assembled = storage.completeFileForItem(item);
        expect(assembled.length()).toEqual(mSampleContentLength);

        String original = CloudUtils.md5(getClass().getResourceAsStream("fred.mp3"));
        expect(CloudUtils.md5(new FileInputStream(assembled))).toEqual(original);
    }

    @Test
    public void shouldReturnMissingIndexes() throws Exception {
        item.setContentLength(TEST_CHUNK_SIZE * 2 + 5);
        Index index = storage.getMissingChunksForItem(item, item.chunkRange(storage.chunkSize));
        expect(index.size()).toEqual(3);
    }
}

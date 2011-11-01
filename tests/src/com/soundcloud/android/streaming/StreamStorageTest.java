package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.fail;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, TEST_CHUNK_SIZE, false);
        item = new StreamItem("fred.mp3");
    }

    private long mSampleContentLength;
    private LinkedHashMap<Integer, ByteBuffer> mSampleBuffers = new LinkedHashMap<Integer, ByteBuffer>();
    private ArrayList<Integer> mSampleChunkIndexes;

    private long setupChunkArray() throws IOException {
        File fred = new File(getClass().getResource("fred.mp3").getFile());
        mSampleContentLength = fred.length();
        FileChannel fc = new FileInputStream(fred).getChannel();
        int chunks = 0;
        mSampleChunkIndexes = new ArrayList<Integer>();
        ByteBuffer buffer = storage.getBuffer();

        while (fc.read(buffer) != -1) {
            mSampleBuffers.put(chunks, clone(buffer));
            mSampleChunkIndexes.add(chunks);
            chunks++;
        }
        return chunks;
    }

    @Test
    public void testSetDataShouldNotStoreIfContentLengthZero() throws IOException {
        expect(item.getContentLength()).toBe(0l);
        expect(storage.storeData(ByteBuffer.wrap(new byte[] {1, 2, 3}), 0, item)).toBeFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDataShouldNotStoreIfDataNull() throws IOException {
        item.setContentLength(10);
        storage.storeData(null, 0, item);
    }

    @Test
    public void shouldWriteAndReadMetadata() throws Exception {
        StreamStorage.Metadata md = new StreamStorage.Metadata();
        md.contentLength = 100;
        md.eTag = "foo";
        md.downloadedChunks = Arrays.asList(1,2,3,4,5);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        md.write(new DataOutputStream(bos));

        StreamStorage.Metadata md_ = StreamStorage.Metadata.read(
                new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        expect(md.contentLength).toEqual(md_.contentLength);
        expect(md.eTag).toEqual(md_.eTag);
        expect(md.downloadedChunks).toEqual(md_.downloadedChunks);
    }


    @Test
    public void shouldSetData() throws Exception {
        item.setContentLength(storage.chunkSize * 2);
        expect(storage.storeData(ByteBuffer.wrap(new byte[]{1, 2, 3}), 0, item)).toBeTrue();
        ByteBuffer data = storage.getChunkData(item, 0);
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
        item.setContentLength(mSampleContentLength);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item, i)).not.toBeNull();
        }
    }

    @Test
    public void shouldTestIncompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        final int writing = mSampleChunkIndexes.size() - 1;
        for (int i = 0; i < writing; i++) {
            storage.storeData(mSampleBuffers.get(mSampleChunkIndexes.get(i)), mSampleChunkIndexes.get(i), item);
        }

        for (int i = 0; i < writing; i++) {
            expect(storage.getChunkData(item, mSampleChunkIndexes.get(i))).not.toBeNull();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldThrowFileNotFoundExceptionIfChunkIsNotAvailable() throws Exception {
       storage.getChunkData(item, 0);
    }


    @Test
    public void shouldTestCompleteSequentialWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            storage.storeData(mSampleBuffers.get(i), i, item);
        }

        for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
            expect(storage.getChunkData(item, i)).not.toBeNull();
        }

        try {
            storage.getChunkData(item, mSampleChunkIndexes.size());
            fail("expected IO exception");
        } catch (IOException e) { /* expected */ }
    }

    @Test
    public void shouldTestCompleteRandomWriting() throws Exception {
        setupChunkArray();
        item.setContentLength(mSampleContentLength);
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer mSampleChunkIndexe1 : mSampleChunkIndexes) {
            storage.storeData(mSampleBuffers.get(mSampleChunkIndexe1), mSampleChunkIndexe1, item);
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
            storage.storeData(mSampleBuffers.get(aChunkArray), aChunkArray, item);
        }

        expect(storage.numberOfChunksForItem(item)).toBe(chunks);

        File assembled = storage.completeFileForItem(item);
        expect(assembled.exists()).toBeTrue();
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

    public static ByteBuffer clone(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

}

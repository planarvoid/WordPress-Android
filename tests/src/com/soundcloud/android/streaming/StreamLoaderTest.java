package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(DefaultTestRunner.class)
public class StreamLoaderTest {
    public static final String TEST_MP3 = "fred.mp3";
    public static final int CHUNK_SIZE = 1024;
    StreamLoader loader;
    StreamStorage storage;
    StreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");
    File testFile;

    private Map<Integer, byte[]> mSampleBuffers = new LinkedHashMap<Integer, byte[]>();
    private List<Integer> mSampleChunkIndexes = new ArrayList<Integer>();

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);
        testFile = new File(getClass().getResource(TEST_MP3).getFile());
        storage = new StreamStorage(DefaultTestRunner.application, baseDir, CHUNK_SIZE);
        loader = new StreamLoader(DefaultTestRunner.application, storage);
        loader.setForceOnline(true);
        item = new StreamItem(TEST_MP3, testFile.length());
    }

    @Test
    public void shouldGetAChunkFromStorage() throws Exception {
        setupChunkArray();
        loader.storeData(mSampleBuffers.get(0), 0, item);
        ByteBuffer actual = loader.getDataForItem(item, Range.from(0, CHUNK_SIZE)).get();
        ByteBuffer expected = readToByteBuffer(testFile, CHUNK_SIZE);

        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldGetAllBytesFromStorage() throws Exception {
        setupChunkArray();
        Collections.shuffle(mSampleChunkIndexes);

        for (Integer i : mSampleChunkIndexes) {
            loader.storeData(mSampleBuffers.get(i), i, item);
        }

        expect(loader.getDataForItem(item, Range.from(0, testFile.length())).get())
            .toEqual(readToByteBuffer(testFile, (int) testFile.length()));
    }


    @Test
    public void shouldReturnAFutureForMissingChunk() throws Exception {
        setupChunkArray();
        mSampleChunkIndexes.remove(0);

        Collections.shuffle(mSampleChunkIndexes);

        for (Integer i : mSampleChunkIndexes) {
            loader.storeData(mSampleBuffers.get(i), i, item);
        }

        addPendingHttpResponse(302, "", new BasicHeader("Location", "foo"));
        addPendingHttpResponse(200, "data");

        StreamFuture cb = loader.getDataForItem(item, Range.from(0, 300));

        expect(loader.getHighPriorityQueue().size()).toBe(1);
        expect(loader.getHighPriorityQueue().head().streamItem.url).toEqual(TEST_MP3);

        expect(cb.isDone()).toBeFalse();
        cb.get(1, TimeUnit.SECONDS);
        expect(cb.isDone()).toBeFalse();
    }

    private int setupChunkArray() throws IOException {
        InputStream is = getClass().getResourceAsStream(TEST_MP3);
        assert is instanceof BufferedInputStream;
        byte[] buffer = new byte[CHUNK_SIZE];
        int chunks = 0;
        while (is.read(buffer, 0, buffer.length) != -1) {
            byte[] copy = new byte[buffer.length];
            System.arraycopy(buffer, 0, copy, 0, buffer.length);
            mSampleBuffers.put(chunks, copy);
            mSampleChunkIndexes.add(chunks);
            chunks++;
        }
        return chunks;
    }

    static ByteBuffer readToByteBuffer(File f, int toRead) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(toRead);
        FileChannel fc = new FileInputStream(f).getChannel();
        fc.read(b);
        b.flip();
        return b;
    }
}

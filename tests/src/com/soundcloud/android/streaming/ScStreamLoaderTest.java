package com.soundcloud.android.streaming;


import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.Range;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(DefaultTestRunner.class)
public class ScStreamLoaderTest {
    ScStreamLoader loader;
    ScStreamStorage storage;
    ScStreamItem item;
    File baseDir = new File(System.getProperty("java.io.tmpdir"), "storage-test");

    @Before
    public void before() {
        CloudUtils.deleteDir(baseDir);

        storage = new ScStreamStorage(DefaultTestRunner.application, baseDir, 1024);
        loader = new ScStreamLoader(DefaultTestRunner.application, storage);
        item = new ScStreamItem(DefaultTestRunner.application, "fred.mp3");
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

    static ByteBuffer readToByteBuffer(InputStream inStream, int toRead) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
        int read;

        while (true) {
            if (toRead < buffer.length){
                read = inStream.read(buffer,0,toRead);
            } else {
                read = inStream.read(buffer);
            }

            if (read == -1)break;
            outStream.write(buffer, 0, read);
            toRead -= read;
            if (toRead <= 0) break;
        }
        ByteBuffer byteData = ByteBuffer.wrap(outStream.toByteArray());
        return byteData;
    }

     @Test
     public void shouldGetAChunkFromStorage() throws Exception {
         setupChunkArray();
         item.setContentLength(mSampleContentLength);

         loader.storeData(mSampleBuffers.get(0), 0, item);
         assertThat(loader.getDataForItem(item,new Range(0, 1024)).get(), equalTo(readToByteBuffer(getClass().getResourceAsStream("fred.mp3"), 1024)));
     }


     @Test
     public void shouldGetAllBytesFromStorage() throws Exception {
         setupChunkArray();
         item.setContentLength(mSampleContentLength);
         Collections.shuffle(mSampleChunkIndexes);

         for (int i = 0; i < mSampleChunkIndexes.size(); i++) {
             loader.storeData(mSampleBuffers.get(mSampleChunkIndexes.get(i)), mSampleChunkIndexes.get(i), item);
         }

         assertThat(loader.getDataForItem(item,new Range(0, (int) mSampleContentLength)).get(), equalTo(readToByteBuffer(getClass().getResourceAsStream("fred.mp3"), (int) mSampleContentLength)));
     }


}

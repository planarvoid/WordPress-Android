package com.soundcloud.android.storage;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JsonFileStorageTest extends AndroidUnitTest {

    private static final String TEST_FILE = "TEST_FILE";
    private JsonFileStorage fileStorage;

    @Before
    public void setUp() throws Exception {
        fileStorage = new JsonFileStorage(context(), new JacksonJsonTransformer());
    }

    @Test
    public void writeAndReadString() throws Exception {
        String data = "SoundCloud";

        fileStorage.writeToFile(TEST_FILE, data);
        fileStorage.readFromFile(TEST_FILE, TypeToken.of(String.class)).test()
                   .assertValue(data);
    }

    @Test
    public void writeAndReadList() throws Exception {
        List<String> data = Arrays.asList("one", "two", "three");

        fileStorage.writeToFile(TEST_FILE, data);
        fileStorage.readFromFile(TEST_FILE, TypeToken.of(List.class)).test()
                   .assertValue(data);
    }

    @Test
    public void readWithoutWriteReturnsEmpty() throws Exception {
        fileStorage.readFromFile(TEST_FILE, TypeToken.of(Date.class)).test()
                   .assertNoValues()
                   .assertCompleted()
                   .assertNoErrors();
    }
}

package com.soundcloud.task;

import com.soundcloud.android.objects.Connection;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestJSON {
    @Test public void testDeserialization() throws IOException {
        String s = "[" +
            " { \"type\": \"bar\", \"id\": 1 }," +
            " { \"type\": \"baz\", \"id\": 2 } " +
          "]";

        List<Connection> l = new ObjectMapper().readValue(s, new TypeReference<List<Connection>>() {});

        assertEquals(2, l.size());

        assertEquals("bar", l.get(0).type);
        assertEquals(1, l.get(0).id);

        assertEquals("baz", l.get(1).type);
        assertEquals(2, l.get(1).id);
    }
}

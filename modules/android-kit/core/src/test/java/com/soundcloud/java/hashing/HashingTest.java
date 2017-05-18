package com.soundcloud.java.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HashingTest {

    @Test
    public void shouldHashStringsWithMD5() {
        assertThat(Hashing.md5("foo")).isEqualTo("acbd18db4cc2f85cedef654fccc4a4d8");
        assertThat(Hashing.md5("000012345")).isEqualTo("4748cdb4de48635e843db0670e1ad47a");
        assertThat(Hashing.md5("00001234588888")).isEqualTo("1dff78cccd58a9a316d872a9d6d08db2");
    }

    @Test
    public void hashingWithMD5ShouldProduceLeadingZerosInHexString() {
        assertThat(Hashing.md5("MYID")).isEqualTo("04ddf8a23b64c654b938b95a50a486f0");
    }
}
package com.soundcloud.android.audio;

import java.nio.ByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: jschmidt
 * Date: 5/14/12
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PlaybackFilter {
    ByteBuffer apply(ByteBuffer buffer, long position, long length);
}

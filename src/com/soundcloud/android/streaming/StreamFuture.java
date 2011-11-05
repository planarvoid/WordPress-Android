package com.soundcloud.android.streaming;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StreamFuture implements Future<ByteBuffer> {
    final StreamItem item;
    final Range byteRange;
    private ByteBuffer byteBuffer;
    private boolean ready;

    public StreamFuture(StreamItem item, Range byteRange) {
        this.item = item;
        this.byteRange = byteRange;
    }

    public synchronized void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        ready = true;
        notifyAll();
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return ready;
    }

    @Override
    public ByteBuffer get() throws InterruptedException {
        try {
            return get(-1);
        } catch (TimeoutException e) {
            throw new InterruptedException(e.getMessage());
        }
    }

    @Override
    public ByteBuffer get(long l, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        return get(timeUnit.toMillis(l));
    }

    private ByteBuffer get(long millis) throws InterruptedException, TimeoutException {
        synchronized (this) {
            while (!ready) {
                if (millis < 0) {
                    wait();
                } else {
                    wait(millis);
                    if (!ready) throw new TimeoutException();
                }
            }
        }
        return byteBuffer;
    }

}

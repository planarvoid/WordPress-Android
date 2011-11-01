package com.soundcloud.android.streaming;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
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

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        ready = true;
        synchronized (this) {
            notifyAll();
        }
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
    public ByteBuffer get() throws InterruptedException, ExecutionException {
        return get(-1);
    }

    @Override
    public ByteBuffer get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return get(timeUnit.toMillis(l));
    }

    private ByteBuffer get(long millis) throws InterruptedException, ExecutionException {
        synchronized (this) {
            while (!ready) {
                try {
                    if (millis < 0) {
                        wait();
                    } else {
                        wait(millis);
                        break;
                    }
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return byteBuffer;
    }

}

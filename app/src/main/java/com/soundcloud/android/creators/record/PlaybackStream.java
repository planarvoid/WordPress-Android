package com.soundcloud.android.creators.record;

import com.soundcloud.android.creators.record.filter.FadeFilter;
import com.soundcloud.android.creators.record.reader.EmptyReader;
import com.soundcloud.android.utils.BufferUtils;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PlaybackStream implements Parcelable {
    private final AudioConfig config;
    private long currentPos;
    private long startPos;
    private long endPos;
    private @NotNull AudioReader playbackFile;

    private PlaybackFilter filter;
    private boolean optimize;
    public static final Parcelable.Creator<PlaybackStream> CREATOR = new Parcelable.Creator<PlaybackStream>() {
        public PlaybackStream createFromParcel(Parcel in) {
            File file = new File(in.readString());

            try {
                PlaybackStream ps = new PlaybackStream(AudioReader.guess(file));
                ps.startPos = in.readLong();
                ps.endPos = in.readLong();
                ps.optimize = in.readInt() == 1;
                ps.filter = in.readParcelable(getClass().getClassLoader());
                ps.refreshTrimWindow();
                return ps;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public PlaybackStream[] newArray(int size) {
            return new PlaybackStream[size];
        }
    };
    private float[] trimWindow = new float[2];

    public PlaybackStream(@NotNull AudioReader audioReader) {
        playbackFile = audioReader;
        config = audioReader.getConfig();
        resetBounds();
        currentPos = -1;
    }

    public void reset() {
        resetBounds();
        filter = null;
        optimize = false;
    }

    public final void resetBounds() {
        startPos = 0;
        endPos = getTotalDuration();
        trimWindow[0] = 0.0f;
        trimWindow[1] = 1.0f;
    }

    public long getDuration() {
        return endPos - startPos;
    }

    public long getPosition() {
        return Math.max(0, currentPos - startPos);
    }

    public AudioConfig getConfig() {
        return config;
    }

    public TrimPreview setStartPositionByPercent(float newPos, long moveTime) {
        if (newPos < 0d || newPos > 1d) {
            throw new IllegalArgumentException("Illegal start percent " + newPos);
        }

        trimWindow[0] = newPos;

        final long old = startPos;
        startPos = (long) (newPos * getTotalDuration());
        return new TrimPreview(this, old, startPos, moveTime);
    }

    public TrimPreview setEndPositionByPercent(float newPos, long moveTime) {
        if (newPos < 0d || newPos > 1d) {
            throw new IllegalArgumentException("Illegal end percent " + newPos);
        }

        trimWindow[1] = newPos;

        final long old = endPos;
        endPos = (long) (newPos * getTotalDuration());
        return new TrimPreview(this, old, endPos, moveTime);
    }

    public int read(ByteBuffer buffer, int bufferSize) throws IOException {
        final int n = playbackFile.read(buffer, bufferSize);
        buffer.flip();
        return n;
    }

    public int readForPlayback(ByteBuffer buffer, int bufferSize) throws IOException {
        if (currentPos < endPos) {
            final int n = playbackFile.read(buffer, bufferSize);
            buffer.flip();
            if (filter != null) {
                filter.apply(buffer, config.msToByte(currentPos - startPos), config.msToByte(endPos - startPos));
            }
            currentPos = playbackFile.getPosition();
            return n;
        }
        return -1;
    }

    public boolean isFinished() {
        return currentPos >= endPos;
    }

    public void resetPlayback() {
        currentPos = -1;
    }

    public void initializePlayback() throws IOException {
        currentPos = getValidPosition(currentPos);
        initializePlayback(currentPos);
    }

    public void initializePlayback(long atPosition) throws IOException {
        playbackFile.seek(atPosition);
    }

    public void close() {
        IOUtils.close(playbackFile);
        playbackFile = new EmptyReader();
    }

    public void setCurrentPosition(long pos) {
        currentPos = pos;
    }

    public void reopen() throws IOException {
        playbackFile.reopen();
        if (currentPos >= 0) {
            playbackFile.seek(currentPos);
        }
        endPos = Math.min(getTotalDuration(), endPos);
    }

    /**
     * @return start position in msecs
     */
    public long getStartPos() {
        return startPos;
    }

    /**
     * @return end position in msecs, or -1 for whole file
     */
    public long getEndPos() {
        return endPos == getTotalDuration() ? -1 : endPos;
    }

    public boolean isOptimized() {
        return optimize;
    }

    public boolean isFading() {
        return filter instanceof FadeFilter;
    }

    public void setFading(boolean enabled) {
        filter = enabled ? new FadeFilter(playbackFile.getConfig()) : null;
    }

    public void setOptimize(boolean enabled) {
        optimize = enabled;
    }

    public void setTrim(long start, long end) {
        startPos = start;
        endPos = end == -1 ? getTotalDuration() : end;
        refreshTrimWindow();
    }

    public long getTrimRight() {
        return getTotalDuration() - endPos;
    }

    public boolean isFiltered() {
        return filter != null || optimize;
    }

    public boolean isTrimmed() {
        return startPos > 0 || (endPos > 0 && endPos < getTotalDuration());
    }

    public boolean isModified() {
        return isTrimmed() || isFiltered();
    }

    public long getTotalDuration() {
        return playbackFile.getDuration();
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public String toString() {
        return "PlaybackStream{" +
                "startPos=" + startPos +
                ", endPos=" + endPos +
                ", playbackFile=" + playbackFile +
                ", filter=" + filter +
                ", optimize=" + optimize +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playbackFile.getFile().getAbsolutePath());
        dest.writeLong(startPos);
        dest.writeLong(endPos);
        dest.writeInt(optimize ? 1 : 0);
        dest.writeParcelable(filter, flags);
    }

    public ByteBuffer buffer() {
        return BufferUtils.allocateAudioBuffer(1024);
    }

    public void refreshTrimWindow() {
        trimWindow[0] = Math.max(0.0f, (float) startPos / getTotalDuration());
        trimWindow[1] = Math.min(1.0f, (float) endPos / getTotalDuration());
    }

    public PlaybackFilter getPlaybackFilter() {
        return filter;
    }

    public float[] getTrimWindow() {
        return trimWindow;
    }

    private long getValidPosition(long currentPosition) {
        return (currentPosition < startPos) ? startPos :
               (currentPosition > endPos) ? endPos : currentPosition;
    }
}

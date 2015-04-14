package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;


public final class UploadEvent {
    private static final int IDLE = 0;
    private static final int UPLOAD_SUCCESS = 1;
    private static final int PROCESSING_STARTED = 2;
    private static final int PROCESSING_PROGRESS = 3;
    private static final int PROCESSING_SUCCESS = 4;
    private static final int PROCESSING_CANCELLED = 5;
    private static final int CANCELLED = 6;
    private static final int TRANSFER_STARTED = 7;
    private static final int TRANSFER_PROGRESS = 8;
    private static final int TRANSFER_SUCCESS = 9;
    private static final int RESIZE_STARTED = 10;
    private static final int RESIZE_SUCCESS = 11;
    private static final int UPLOAD_CANCELLED = 12;
    private static final int TRANSFER_CANCELLED = 13;
    private static final int ERROR = 14;
    private static final int UPLOAD_START = 15;

    private final int kind;
    private final Recording recording;
    private final PublicApiTrack track;
    private final int progress;

    public static UploadEvent idle() {
        return new UploadEvent(IDLE);
    }

    public static UploadEvent error(Recording recording) {
        return new UploadEvent(ERROR, recording);
    }

    public static UploadEvent cancelled(Recording recording) {
        return new UploadEvent(CANCELLED, recording);
    }

    public static UploadEvent success(Recording recording) {
        return new UploadEvent(UPLOAD_SUCCESS, recording);
    }

    public static UploadEvent processingStarted(Recording recording) {
        return new UploadEvent(PROCESSING_STARTED, recording, -1);
    }

    public static UploadEvent processingSuccess(Recording recording) {
        return new UploadEvent(PROCESSING_SUCCESS, recording, 100);
    }

    public static UploadEvent processingProgress(Recording recording, int progress) {
        return new UploadEvent(PROCESSING_PROGRESS, recording, progress);
    }

    public static UploadEvent transferStarted(Recording recording) {
        return new UploadEvent(TRANSFER_STARTED, recording, -1);
    }

    public static UploadEvent transferProgress(Recording recording, int progress) {
        return new UploadEvent(TRANSFER_PROGRESS, recording, progress);
    }

    public static UploadEvent transferSuccess(Recording recording, PublicApiTrack track) {
        return new UploadEvent(TRANSFER_SUCCESS, recording, 100, track);
    }

    public static UploadEvent resizeStarted(Recording recording) {
        return new UploadEvent(RESIZE_STARTED, recording);
    }

    public static UploadEvent resizeSuccess(Recording recording) {
        return new UploadEvent(RESIZE_SUCCESS, recording);
    }

    public static UploadEvent start(Recording recording) {
        return new UploadEvent(UPLOAD_START, recording);
    }

    private UploadEvent(int kind) {
        this(kind, null);
    }

    private UploadEvent(int kind, Recording recording) {
        this(kind, recording, 0);
    }

    private UploadEvent(int kind, Recording recording, int progress) {
        this(kind, recording, progress, null);
    }

    private UploadEvent(int kind, Recording recording, int progress, PublicApiTrack track) {
        this.kind = kind;
        this.recording = recording;
        this.track = track;
        this.progress = progress;
    }

    public boolean isUploading() {
        return !(kind == IDLE || isCancelled() || isError() || isUploadSuccess());
    }

    public boolean isFinished() {
        return isUploadSuccess() || isError();
    }

    public boolean isStarted() {
        return kind == UPLOAD_START;
    }

    public boolean isError() {
        return kind == ERROR;
    }

    public boolean isUploadSuccess() {
        return kind == UPLOAD_SUCCESS;
    }

    public boolean isProcessing() {
        return isProcessingProgress() || isProcessingStarted() || isProcessingSuccess();
    }

    public boolean isTransfer() {
        return isTransferStarted() || isTransferProgress() || isTransferSuccess();
    }

    public boolean isProcessingStarted() {
        return kind == PROCESSING_STARTED;
    }

    public boolean isProcessingProgress() {
        return kind == PROCESSING_PROGRESS;
    }

    public boolean isProcessingSuccess() {
        return kind == PROCESSING_SUCCESS;
    }

    public boolean isTransferStarted() {
        return kind == TRANSFER_STARTED;
    }

    public boolean isTransferProgress() {
        return kind == TRANSFER_PROGRESS;
    }

    public boolean isTransferSuccess() {
        return kind == TRANSFER_SUCCESS;
    }

    public boolean isCancelled() {
        return kind == CANCELLED || isTransferCancelled() || isUploadCancelled();
    }

    public boolean isResizeStarted() {
        return kind == RESIZE_STARTED;
    }

    public boolean isResizeSuccess() {
        return kind == RESIZE_SUCCESS;
    }

    public boolean isUploadCancelled() {
        return kind == UPLOAD_CANCELLED;
    }

    public boolean isTransferCancelled() {
        return kind == TRANSFER_CANCELLED;
    }

    public Recording getRecording() {
        return recording;
    }

    public int getProgress() {
        return progress;
    }

    public PublicApiTrack getTrack() {
        return track;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UploadEvent that = (UploadEvent) o;

        return Objects.equal(that.kind, this.kind)
                && Objects.equal(that.recording, this.recording)
                && Objects.equal(that.progress, this.progress)
                && Objects.equal(that.track, this.track);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(kind, recording, progress, track);
    }

    @Override
    public String toString() {
        return String.format("Upload Event with type id %d, progress = %d and track = %s", kind, progress, track != null ? track.getUrn() : "none");
    }
}

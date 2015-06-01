package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;

public final class UploadEvent {
    private static final String IDLE = "idle";
    private static final String UPLOAD_SUCCESS = "upload_success";
    private static final String PROCESSING_STARTED = "processing_started";
    private static final String PROCESSING_PROGRESS = "processing_progress";
    private static final String PROCESSING_SUCCESS = "processing_success";
    private static final String PROCESSING_CANCELLED = "processing_cancelled";
    private static final String CANCELLED = "cancelled";
    private static final String TRANSFER_STARTED = "transfer_started";
    private static final String TRANSFER_PROGRESS = "transfer_progress";
    private static final String TRANSFER_SUCCESS = "transfer_success";
    private static final String RESIZE_STARTED = "resize_started";
    private static final String RESIZE_SUCCESS = "resize_success";
    private static final String UPLOAD_CANCELLED = "upload_cancelled";
    private static final String TRANSFER_CANCELLED = "transfer_cancelled";
    private static final String ERROR = "error";
    private static final String UPLOAD_START = "upload_start";

    private final String kind;
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

    private UploadEvent(String kind) {
        this(kind, null);
    }

    private UploadEvent(String kind, Recording recording) {
        this(kind, recording, 0);
    }

    private UploadEvent(String kind, Recording recording, int progress) {
        this(kind, recording, progress, null);
    }

    private UploadEvent(String kind, Recording recording, int progress, PublicApiTrack track) {
        this.kind = kind;
        this.recording = recording;
        this.track = track;
        this.progress = progress;
    }

    public boolean isUploading() {
        return !(kind.equals(IDLE) || isCancelled() || isError() || isUploadSuccess());
    }

    public boolean isFinished() {
        return isUploadSuccess() || isError();
    }

    public boolean isStarted() {
        return kind.equals(UPLOAD_START);
    }

    public boolean isError() {
        return kind.equals(ERROR);
    }

    public boolean isUploadSuccess() {
        return kind.equals(UPLOAD_SUCCESS);
    }

    public boolean isProcessing() {
        return isProcessingProgress() || isProcessingStarted() || isProcessingSuccess();
    }

    public boolean isTransfer() {
        return isTransferStarted() || isTransferProgress() || isTransferSuccess();
    }

    public boolean isProcessingStarted() {
        return kind.equals(PROCESSING_STARTED);
    }

    public boolean isProcessingProgress() {
        return kind.equals(PROCESSING_PROGRESS);
    }

    public boolean isProcessingSuccess() {
        return kind.equals(PROCESSING_SUCCESS);
    }

    public boolean isTransferStarted() {
        return kind.equals(TRANSFER_STARTED);
    }

    public boolean isTransferProgress() {
        return kind.equals(TRANSFER_PROGRESS);
    }

    public boolean isTransferSuccess() {
        return kind.equals(TRANSFER_SUCCESS);
    }

    public boolean isCancelled() {
        return kind.equals(CANCELLED) || isTransferCancelled() || isUploadCancelled();
    }

    public boolean isResizeStarted() {
        return kind.equals(RESIZE_STARTED);
    }

    public boolean isResizeSuccess() {
        return kind.equals(RESIZE_SUCCESS);
    }

    public boolean isUploadCancelled() {
        return kind.equals(UPLOAD_CANCELLED);
    }

    public boolean isTransferCancelled() {
        return kind.equals(TRANSFER_CANCELLED);
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
        return Objects.toStringHelper(this)
                .add("kind", kind)
                .add("progress", progress)
                .add("track", track)
                .toString();
    }
}

package com.soundcloud.android.service.record;

import com.soundcloud.android.model.Upload;
interface ICloudCreateService
{
  void startReading();
  void startRecording(String path);
  boolean isRecording();
  String getRecordingPath();
  void stopRecording();
  void loadPlaybackTrack(String playbackFile);
  boolean isPlayingBack();
  void setPlaybackStart(float pos);
  void setPlaybackEnd(float pos);
  void startPlayback();
  void pausePlayback();
  void stopPlayback();
  long getCurrentPlaybackPosition();
  float getCurrentProgressPercent();
  long getPlaybackDuration();
  void seekTo(int position);
  String getPlaybackPath();
  long getPlaybackLocalId();
  boolean startUpload(in Upload upload);
  boolean isUploading();
  void cancelUpload();
  void cancelUploadById(long id);
  Upload getUploadById(long id);
  long getUploadLocalId();
}


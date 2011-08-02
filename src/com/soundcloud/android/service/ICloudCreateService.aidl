package com.soundcloud.android.service;

import com.soundcloud.android.model.Upload;
interface ICloudCreateService
{
  void startRecording(String path, int mode);
  boolean isRecording();
  String getRecordingPath();
  void stopRecording();
  void updateRecordTicker();
  void loadPlaybackTrack(String playbackFile);
  boolean isPlayingBack();
  void startPlayback();
  void pausePlayback();
  void stopPlayback();
  int getCurrentPlaybackPosition();
  int getPlaybackDuration();
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


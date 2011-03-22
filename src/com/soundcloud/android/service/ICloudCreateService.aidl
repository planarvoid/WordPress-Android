package com.soundcloud.android.service;

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
  void uploadTrack(in Map trackdata);
  boolean isUploading();
  void cancelUpload();
  void setCurrentState(int newState);
  int getCurrentState();
}


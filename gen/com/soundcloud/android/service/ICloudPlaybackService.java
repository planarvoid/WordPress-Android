/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /sc_workspace/Soundcloud_Android/src/com/soundcloud/android/service/ICloudPlaybackService.aidl
 */
package com.soundcloud.android.service;
public interface ICloudPlaybackService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.soundcloud.android.service.ICloudPlaybackService
{
private static final java.lang.String DESCRIPTOR = "com.soundcloud.android.service.ICloudPlaybackService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.soundcloud.android.service.ICloudPlaybackService interface,
 * generating a proxy if needed.
 */
public static com.soundcloud.android.service.ICloudPlaybackService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.soundcloud.android.service.ICloudPlaybackService))) {
return ((com.soundcloud.android.service.ICloudPlaybackService)iin);
}
return new com.soundcloud.android.service.ICloudPlaybackService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_openFile:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track _arg0;
if ((0!=data.readInt())) {
_arg0 = com.soundcloud.android.objects.Track.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
boolean _arg1;
_arg1 = (0!=data.readInt());
this.openFile(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_enqueueTrack:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track _arg0;
if ((0!=data.readInt())) {
_arg0 = com.soundcloud.android.objects.Track.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
int _arg1;
_arg1 = data.readInt();
this.enqueueTrack(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_enqueue:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track[] _arg0;
_arg0 = data.createTypedArray(com.soundcloud.android.objects.Track.CREATOR);
int _arg1;
_arg1 = data.readInt();
this.enqueue(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_clearQueue:
{
data.enforceInterface(DESCRIPTOR);
this.clearQueue();
reply.writeNoException();
return true;
}
case TRANSACTION_getQueuePosition:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getQueuePosition();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isPlaying:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPlaying();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_pause:
{
data.enforceInterface(DESCRIPTOR);
this.pause();
reply.writeNoException();
return true;
}
case TRANSACTION_play:
{
data.enforceInterface(DESCRIPTOR);
this.play();
reply.writeNoException();
return true;
}
case TRANSACTION_prev:
{
data.enforceInterface(DESCRIPTOR);
this.prev();
reply.writeNoException();
return true;
}
case TRANSACTION_next:
{
data.enforceInterface(DESCRIPTOR);
this.next();
reply.writeNoException();
return true;
}
case TRANSACTION_duration:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.duration();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_position:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.position();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_loadPercent:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.loadPercent();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_seek:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _result = this.seek(_arg0);
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_getTrack:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Track _result = this.getTrack();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getTrackName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getTrackName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getTrackId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getTrackId();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getUserName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getUserName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getUserPermalink:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getUserPermalink();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getWaveformUrl:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getWaveformUrl();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_isAsyncOpening:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isAsyncOpening();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setComments:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Comment[] _arg0;
_arg0 = data.createTypedArray(com.soundcloud.android.objects.Comment.CREATOR);
java.lang.String _arg1;
_arg1 = data.readString();
this.setComments(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_addComment:
{
data.enforceInterface(DESCRIPTOR);
com.soundcloud.android.objects.Comment _arg0;
if ((0!=data.readInt())) {
_arg0 = com.soundcloud.android.objects.Comment.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.addComment(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setFavoriteStatus:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.setFavoriteStatus(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getQueue:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.soundcloud.android.objects.Track> _result = this.getQueue();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
case TRANSACTION_moveQueueItem:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.moveQueueItem(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_setQueuePosition:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setQueuePosition(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getPath:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getPath();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getDuration:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getDuration();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getDownloadable:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getDownloadable();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_removeTracks:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
int _result = this.removeTracks(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_removeTrack:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _result = this.removeTrack(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.soundcloud.android.service.ICloudPlaybackService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void openFile(com.soundcloud.android.objects.Track track, boolean oneShot) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((track!=null)) {
_data.writeInt(1);
track.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(((oneShot)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_openFile, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void enqueueTrack(com.soundcloud.android.objects.Track track, int action) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((track!=null)) {
_data.writeInt(1);
track.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(action);
mRemote.transact(Stub.TRANSACTION_enqueueTrack, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void enqueue(com.soundcloud.android.objects.Track[] trackData, int playPos) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedArray(trackData, 0);
_data.writeInt(playPos);
mRemote.transact(Stub.TRANSACTION_enqueue, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void clearQueue() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_clearQueue, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public int getQueuePosition() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getQueuePosition, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean isPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPlaying, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void pause() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void play() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_play, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void prev() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_prev, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void next() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_next, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public long duration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_duration, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public long position() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_position, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int loadPercent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_loadPercent, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public long seek(long pos) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(pos);
mRemote.transact(Stub.TRANSACTION_seek, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public com.soundcloud.android.objects.Track getTrack() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
com.soundcloud.android.objects.Track _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTrack, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = com.soundcloud.android.objects.Track.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getTrackName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTrackName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getTrackId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTrackId, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getUserName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getUserName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getUserPermalink() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getUserPermalink, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getWaveformUrl() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getWaveformUrl, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean isAsyncOpening() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isAsyncOpening, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void setComments(com.soundcloud.android.objects.Comment[] commentData, java.lang.String trackId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedArray(commentData, 0);
_data.writeString(trackId);
mRemote.transact(Stub.TRANSACTION_setComments, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void addComment(com.soundcloud.android.objects.Comment commentData) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((commentData!=null)) {
_data.writeInt(1);
commentData.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_addComment, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void setFavoriteStatus(java.lang.String trackId, java.lang.String favoriteStatus) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(trackId);
_data.writeString(favoriteStatus);
mRemote.transact(Stub.TRANSACTION_setFavoriteStatus, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public java.util.List<com.soundcloud.android.objects.Track> getQueue() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<com.soundcloud.android.objects.Track> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getQueue, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(com.soundcloud.android.objects.Track.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void moveQueueItem(int from, int to) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(from);
_data.writeInt(to);
mRemote.transact(Stub.TRANSACTION_moveQueueItem, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void setQueuePosition(int index) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(index);
mRemote.transact(Stub.TRANSACTION_setQueuePosition, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public java.lang.String getPath() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPath, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getDuration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getDuration, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getDownloadable() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getDownloadable, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int removeTracks(int first, int last) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(first);
_data.writeInt(last);
mRemote.transact(Stub.TRANSACTION_removeTracks, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int removeTrack(java.lang.String id) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(id);
mRemote.transact(Stub.TRANSACTION_removeTrack, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_openFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_enqueueTrack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_enqueue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_clearQueue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getQueuePosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_isPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_pause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_play = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_prev = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_next = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_duration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_position = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_loadPercent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_seek = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_getTrack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_getTrackName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_getTrackId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_getUserName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_getUserPermalink = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_getWaveformUrl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_isAsyncOpening = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_setComments = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
static final int TRANSACTION_addComment = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
static final int TRANSACTION_setFavoriteStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
static final int TRANSACTION_getQueue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
static final int TRANSACTION_moveQueueItem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
static final int TRANSACTION_setQueuePosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
static final int TRANSACTION_getPath = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
static final int TRANSACTION_getDuration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
static final int TRANSACTION_getDownloadable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
static final int TRANSACTION_removeTracks = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
static final int TRANSACTION_removeTrack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
}
public void openFile(com.soundcloud.android.objects.Track track, boolean oneShot) throws android.os.RemoteException;
public void enqueueTrack(com.soundcloud.android.objects.Track track, int action) throws android.os.RemoteException;
public void enqueue(com.soundcloud.android.objects.Track[] trackData, int playPos) throws android.os.RemoteException;
public void clearQueue() throws android.os.RemoteException;
public int getQueuePosition() throws android.os.RemoteException;
public boolean isPlaying() throws android.os.RemoteException;
public void stop() throws android.os.RemoteException;
public void pause() throws android.os.RemoteException;
public void play() throws android.os.RemoteException;
public void prev() throws android.os.RemoteException;
public void next() throws android.os.RemoteException;
public long duration() throws android.os.RemoteException;
public long position() throws android.os.RemoteException;
public int loadPercent() throws android.os.RemoteException;
public long seek(long pos) throws android.os.RemoteException;
public com.soundcloud.android.objects.Track getTrack() throws android.os.RemoteException;
public java.lang.String getTrackName() throws android.os.RemoteException;
public java.lang.String getTrackId() throws android.os.RemoteException;
public java.lang.String getUserName() throws android.os.RemoteException;
public java.lang.String getUserPermalink() throws android.os.RemoteException;
public java.lang.String getWaveformUrl() throws android.os.RemoteException;
public boolean isAsyncOpening() throws android.os.RemoteException;
public void setComments(com.soundcloud.android.objects.Comment[] commentData, java.lang.String trackId) throws android.os.RemoteException;
public void addComment(com.soundcloud.android.objects.Comment commentData) throws android.os.RemoteException;
public void setFavoriteStatus(java.lang.String trackId, java.lang.String favoriteStatus) throws android.os.RemoteException;
public java.util.List<com.soundcloud.android.objects.Track> getQueue() throws android.os.RemoteException;
public void moveQueueItem(int from, int to) throws android.os.RemoteException;
public void setQueuePosition(int index) throws android.os.RemoteException;
public java.lang.String getPath() throws android.os.RemoteException;
public java.lang.String getDuration() throws android.os.RemoteException;
public java.lang.String getDownloadable() throws android.os.RemoteException;
public int removeTracks(int first, int last) throws android.os.RemoteException;
public int removeTrack(java.lang.String id) throws android.os.RemoteException;
}

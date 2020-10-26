/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.screening;

import net.majorkernelpanic.screening.audio.AACStream;
import net.majorkernelpanic.screening.audio.AMRNBStream;
import net.majorkernelpanic.screening.audio.AudioQuality;
import net.majorkernelpanic.screening.audio.AudioStream;
import net.majorkernelpanic.screening.video.H263Stream;
import net.majorkernelpanic.screening.video.H264Stream;
import net.majorkernelpanic.screening.video.VideoQuality;
import net.majorkernelpanic.screening.video.VideoStream;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {
  public final static String TAG = "SessionBuilder";

  /** Can be used with {@link #setVideoEncoder}. */
  public final static int VIDEO_NONE = 0;

  /** Can be used with {@link #setVideoEncoder}. */
  public final static int VIDEO_H264 = 1;

  /** Can be used with {@link #setVideoEncoder}. */
  public final static int VIDEO_H263 = 2;

  /** Can be used with {@link #setAudioEncoder}. */
  public final static int AUDIO_NONE = 0;

  /** Can be used with {@link #setAudioEncoder}. */
  public final static int AUDIO_AMRNB = 3;

  /** Can be used with {@link #setAudioEncoder}. */
  public final static int AUDIO_AAC = 5;

  // Default configuration
  private VideoQuality mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;
  private AudioQuality mAudioQuality = AudioQuality.DEFAULT_AUDIO_QUALITY;
  private Context mContext;
  private int mVideoEncoder = VIDEO_H263;
  private int mAudioEncoder = AUDIO_NONE;
  private int mTimeToLive = 64;
  private String mOrigin = null;
  private String mDestination = null;
  private Session.Callback mCallback = null;

  // Removes the default public constructor
  private SessionBuilder() {}

  // The SessionManager implements the singleton pattern
  private static volatile SessionBuilder sInstance = null;

  /**
   * Returns a reference to the {@link SessionBuilder}.
   * @return The reference to the {@link SessionBuilder}
   */
  public final static SessionBuilder getInstance() {
    if (sInstance == null) {
      synchronized (SessionBuilder.class) {
        if (sInstance == null) {
          SessionBuilder.sInstance = new SessionBuilder();
        }
      }
    }
    return sInstance;
  }

  /**
   * Creates a new {@link Session}.
   * @return The new Session
   * @throws IOException
   */
  public Session build() {
    Session session;

    session = new Session();
    session.setOrigin(mOrigin);
    session.setDestination(mDestination);
    session.setTimeToLive(mTimeToLive);
    session.setCallback(mCallback);

    switch (mVideoEncoder) {
      case VIDEO_H263:
        session.addVideoTrack(new H263Stream());
        break;
      case VIDEO_H264:
        session.addVideoTrack(new H264Stream());
        break;
    }

    switch (mAudioEncoder) {
      case AUDIO_AAC:
        session.addAudioTrack(new AACStream());
        break;
      case AUDIO_AMRNB:
        session.addAudioTrack(new AMRNBStream());
        break;
    }

    if (session.getVideoTrack() != null) {
      VideoStream video = session.getVideoTrack();
      video.setVideoQuality(mVideoQuality);
      video.setDestinationPorts(5006);

      if (mContext != null)
        video.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
    }

    if (session.getAudioTrack() != null) {
      AudioStream audio = session.getAudioTrack();
      audio.setAudioQuality(mAudioQuality);
      audio.setDestinationPorts(5004);

      if ((mContext != null) && (mAudioEncoder == AUDIO_AAC))
        ((AACStream) audio).setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
    }

    return session;
  }

  /**
   * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
   * Note that you should pass the Application context, not the context of an Activity.
   **/
  public SessionBuilder setContext(Context context) {
    mContext = context;

    if (VideoQuality.DEFAULT_VIDEO_QUALITY == null)
      initVideoQuality(mContext);

    if (mVideoQuality == null)
      mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;

    return this;
  }

  public SessionBuilder setMediaProjection(int resultCode, Intent resultData) {
    if (mContext == null)
      throw new RuntimeException("MediaProjection requires a Context");

    initMediaProjection(mContext, resultCode, resultData);

    return this;
  }

  /** Sets the destination of the session. */
  public SessionBuilder setDestination(String destination) {
    mDestination = destination;
    return this;
  }

  /** Sets the origin of the session. It appears in the SDP of the session. */
  public SessionBuilder setOrigin(String origin) {
    mOrigin = origin;
    return this;
  }

  /** Sets the video stream quality. */
  public SessionBuilder setVideoQuality(VideoQuality quality) {
    mVideoQuality = quality.clone();
    return this;
  }

  /** Sets the video stream quality. */
  public SessionBuilder setVideoQuality(int framerate, int bitrate) {
    mVideoQuality = VideoQuality.extend(framerate, bitrate);
    return this;
  }

  /** Sets the audio encoder. */
  public SessionBuilder setAudioEncoder(int encoder) {
    mAudioEncoder = encoder;
    return this;
  }

  /** Sets the audio quality. */
  public SessionBuilder setAudioQuality(AudioQuality quality) {
    mAudioQuality = quality.clone();
    return this;
  }

  /** Sets the audio quality. */
  public SessionBuilder setAudioQuality(int samplerate, int bitrate) {
    mAudioQuality = new AudioQuality(samplerate, bitrate);
    return this;
  }

  /** Sets the default video encoder. */
  public SessionBuilder setVideoEncoder(int encoder) {
    mVideoEncoder = encoder;
    return this;
  }

  public SessionBuilder setTimeToLive(int ttl) {
    mTimeToLive = ttl;
    return this;
  }

  public SessionBuilder setCallback(Session.Callback callback) {
    mCallback = callback;
    return this;
  }

  /** Returns the context set with {@link #setContext(Context)}*/
  public Context getContext() {
    return mContext;
  }

  /** Returns the destination ip address set with {@link #setDestination(String)}. */
  public String getDestination() {
    return mDestination;
  }

  /** Returns the origin ip address set with {@link #setOrigin(String)}. */
  public String getOrigin() {
    return mOrigin;
  }

  /** Returns the audio encoder set with {@link #setAudioEncoder(int)}. */
  public int getAudioEncoder() {
    return mAudioEncoder;
  }

  /** Returns the video encoder set with {@link #setVideoEncoder(int)}. */
  public int getVideoEncoder() {
    return mVideoEncoder;
  }

  /** Returns the VideoQuality set with {@link #setVideoQuality(VideoQuality)}. */
  public VideoQuality getVideoQuality() {
    return mVideoQuality;
  }

  /** Returns the AudioQuality set with {@link #setAudioQuality(AudioQuality)}. */
  public AudioQuality getAudioQuality() {
    return mAudioQuality;
  }

  /** Returns the time to live set with {@link #setTimeToLive(int)}. */
  public int getTimeToLive() {
    return mTimeToLive;
  }

  /** Returns a new {@link SessionBuilder} with the same configuration. */
  public SessionBuilder clone() {
    return new SessionBuilder()
    .setDestination(mDestination)
    .setOrigin(mOrigin)
    .setVideoQuality(mVideoQuality)
    .setVideoEncoder(mVideoEncoder)
    .setTimeToLive(mTimeToLive)
    .setAudioEncoder(mAudioEncoder)
    .setAudioQuality(mAudioQuality)
    .setContext(mContext)
    .setCallback(mCallback);
  }

  public static void initVideoQuality(Context mContext) {
    DisplayMetrics metrics = new DisplayMetrics();
    WindowManager window = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    window.getDefaultDisplay().getRealMetrics(metrics);

    int screenWidth  = metrics.widthPixels;
    int screenHeight = metrics.heightPixels;
    int screenDpi    = metrics.densityDpi;

    VideoQuality.init(screenWidth, screenHeight, screenDpi);
  }

  public static void initMediaProjection(Context mContext, int resultCode, Intent resultData) {
    MediaProjectionManager mediaProjectionManager  = (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    MediaProjection        mediaProjection         = mediaProjectionManager.getMediaProjection(resultCode, resultData);

    VideoStream.init(mediaProjection);
  }

  public static String[] getRequiredPermissions() {
    return getRequiredPermissions(getInstance());
  }

  public static String[] getRequiredPermissions(SessionBuilder builder) {
    if (builder == null)
      builder = getInstance();

    ArrayList<String> requiredPermissions = new ArrayList<>();

    if (builder.getAudioEncoder() != SessionBuilder.AUDIO_NONE)
      requiredPermissions.add("android.permission.RECORD_AUDIO");

    if (builder.getAudioEncoder() == SessionBuilder.AUDIO_AAC)
      requiredPermissions.add("android.permission.WRITE_EXTERNAL_STORAGE");

    return requiredPermissions.toArray(new String[requiredPermissions.size()]);
  }
}

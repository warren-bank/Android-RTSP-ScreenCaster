/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
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

package net.majorkernelpanic.screening.video;

import net.majorkernelpanic.screening.MediaStream;
import net.majorkernelpanic.screening.Stream;
import net.majorkernelpanic.screening.hw.EncoderDebugger;
import net.majorkernelpanic.screening.rtp.MediaCodecInputStream;

import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.InputStream;

/**
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {
  protected final static String TAG = "VideoStream";

  /** Default video stream quality. */
  private static MediaProjection MEDIA_PROJECTION = null;

  public static void init(MediaProjection mp) {
    MEDIA_PROJECTION = mp;
  }

  protected VideoQuality      mRequestedQuality = null;
  protected VideoQuality      mQuality          = null;
  protected SharedPreferences mSettings         = null;

  private Surface             inputSurface      = null;
  private VirtualDisplay      virtualDisplay    = null;

  public VideoStream() {
    super();

    mRequestedMode    = MODE_MEDIACODEC_API;
    mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
  }

  /**
   * Sets the configuration of the stream. You can call this method at any time
   * and changes will take effect next time you call {@link #configure()}.
   * @param videoQuality Quality of the stream
   */
  public void setVideoQuality(VideoQuality videoQuality) {
    if (!mRequestedQuality.equals(videoQuality)) {
      mRequestedQuality = videoQuality.clone();
    }
  }

  /**
   * Returns the quality of the stream.
   */
  public VideoQuality getVideoQuality() {
    return mRequestedQuality;
  }

  /**
   * Some data (SPS and PPS params) needs to be stored when {@link #getSessionDescription()} is called
   * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
   */
  public void setPreferences(SharedPreferences prefs) {
    mSettings = prefs;
  }

  /**
   * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
   * to apply your configuration of the stream.
   */
  public synchronized void configure() throws IllegalStateException, IOException {
    super.configure();
    mQuality = mRequestedQuality.clone();
  }

  /**
   * Starts the stream.
   */
  public synchronized void start() throws IllegalStateException, IOException {
    super.start();
    Log.d(TAG,"Stream configuration: FPS: "+mQuality.framerate+" Width: "+mQuality.screenWidth+" Height: "+mQuality.screenHeight);
  }

  /** Stops the stream. */
  public synchronized void stop() {
    if (virtualDisplay != null) {
      virtualDisplay.release();
      virtualDisplay = null;
    }
    if (inputSurface != null) {
      inputSurface.release();
      inputSurface = null;
    }
    super.stop();
  }

  protected void encodeWithMediaRecorder() throws RuntimeException, IOException {
    encodeWithMediaCodec();
  }

  /**
   * Video encoding is done by a MediaCodec.
   */
  protected void encodeWithMediaCodec() throws RuntimeException, IOException {
    Log.d(TAG,"Video encoded using the MediaCodec API with a surface");

    if (MEDIA_PROJECTION == null)
      throw new RuntimeException("VideoStream requires a MediaProjection");

    String videoFormat      = MediaFormat.MIMETYPE_VIDEO_AVC;  // "video/avc"
    MediaFormat mediaFormat = MediaFormat.createVideoFormat(videoFormat, mQuality.screenWidth, mQuality.screenHeight);

    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,       mQuality.framerate);
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,         mQuality.bitrate);
    mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,    0);
    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,     MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

  //mMediaCodec = MediaCodec.createEncoderByType(videoFormat);

    EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.screenWidth, mQuality.screenHeight);
    mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());

    mMediaCodec.configure(mediaFormat, /* surface= */ null, /* crypto= */ null, MediaCodec.CONFIGURE_FLAG_ENCODE);

    inputSurface = mMediaCodec.createInputSurface();
    mMediaCodec.start();

    int flags      = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
    virtualDisplay = MEDIA_PROJECTION.createVirtualDisplay("ScreenCaster", mQuality.screenWidth, mQuality.screenHeight, mQuality.screenDpi, flags, inputSurface, /* callback= */ null, /* handler= */ null);

    // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
    mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
    mPacketizer.start();
  }

  /**
   * Returns a description of the stream using SDP.
   * This method can only be called after {@link Stream#configure()}.
   * @throws IllegalStateException Thrown when {@link Stream#configure()} wa not called.
   */
  public abstract String getSessionDescription() throws IllegalStateException;
}

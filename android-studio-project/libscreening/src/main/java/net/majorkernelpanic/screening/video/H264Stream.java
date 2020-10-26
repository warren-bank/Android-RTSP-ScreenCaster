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

package net.majorkernelpanic.screening.video;

import net.majorkernelpanic.screening.Session;
import net.majorkernelpanic.screening.SessionBuilder;
import net.majorkernelpanic.screening.hw.EncoderDebugger;
import net.majorkernelpanic.screening.mp4.MP4Config;
import net.majorkernelpanic.screening.rtp.H264Packetizer;

import android.annotation.SuppressLint;
import android.content.SharedPreferences.Editor;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A class for streaming H.264 from the display screen of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setVideoQuality(VideoQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class H264Stream extends VideoStream {
  public final static String TAG = "H264Stream";

  private Semaphore mLock = new Semaphore(0);
  private MP4Config mConfig;

  /**
   * Constructs the H.264 stream.
   * @throws IOException
   */
  public H264Stream() {
    super();
    mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    mPacketizer   = new H264Packetizer();
  }

  /**
   * Returns a description of the stream using SDP. It can then be included in an SDP file.
   */
  public synchronized String getSessionDescription() throws IllegalStateException {
    if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
    return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
    "a=rtpmap:96 H264/90000\r\n" +
    "a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";
  }

  /**
   * Starts the stream.
   */
  public synchronized void start() throws IllegalStateException, IOException {
    if (!mStreaming) {
      if (mConfig == null)
        updateMP4Config();

      if (mConfig != null) {
        byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
        byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
        ((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
      }
    }
    super.start();
  }

  /**
   * Configures the stream. You need to call this before calling {@link #getSessionDescription()} to apply
   * your configuration of the stream.
   */
  public synchronized void configure() throws IllegalStateException, IOException {
    super.configure();
    updateMP4Config();
  }

  private void updateMP4Config() throws IllegalStateException, IOException {
    mConfig = testH264();
  }

  /**
   * Tests if streaming with the given configuration (bit rate, frame rate, resolution) is possible
   * and determines the pps and sps. Should not be called by the UI thread.
   **/
  private MP4Config testH264() throws IllegalStateException, IOException {
    if (mQuality.screenWidth >= 640) {
      // MediaCodec API is too slow for high resolutions
      mMode = MODE_MEDIARECORDER_API;
    }

    return (mMode == MODE_MEDIARECORDER_API)
      ? testMediaRecorderAPI()
      : testMediaCodecAPI()
    ;
  }

  @SuppressLint("NewApi")
  private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
    try {
      EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.screenWidth, mQuality.screenHeight);
      return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
    } catch (Exception e) {
      Log.e(TAG,"Resolution not supported with the MediaCodec API.");

      // Fallback to the MediaRecorder API
      mMode = MODE_MEDIARECORDER_API;
      return testH264();
    }
  }

  // Should not be called by the UI thread
  private MP4Config testMediaRecorderAPI() throws RuntimeException, IOException {
    String key = PREF_PREFIX+"h264-mr-"+mRequestedQuality.framerate+","+mRequestedQuality.screenWidth+","+mRequestedQuality.screenHeight;

    if (mSettings != null && mSettings.contains(key) ) {
      String[] s = mSettings.getString(key, "").split(",");
      return new MP4Config(s[0],s[1],s[2]);
    }

    if (MEDIA_PROJECTION == null)
      throw new RuntimeException("VideoStream requires a MediaProjection");

    if (CACHE_DIR == null)
      throw new RuntimeException("Application cache directory is not configured. Context is required.");

    final String TESTFILE = CACHE_DIR.getPath()+"/spydroid-test.mp4";

    Log.i(TAG,"Testing H264 support... Test file saved at: "+TESTFILE);

    try {
      File file = new File(TESTFILE);
      file.createNewFile();
    } catch (IOException e) {
      throw e;
    }

    MediaRecorder   mediaRecorder  = null;
    Surface         videoSurface   = null;
    VirtualDisplay  virtualDisplay = null;

    try {
      mediaRecorder = new MediaRecorder();
      mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mediaRecorder.setVideoEncoder(mVideoEncoder);
      mediaRecorder.setVideoSize(mRequestedQuality.screenWidth, mRequestedQuality.screenHeight);
      mediaRecorder.setVideoFrameRate(mRequestedQuality.framerate);
      mediaRecorder.setVideoEncodingBitRate((int)(mRequestedQuality.bitrate*0.8));

      mediaRecorder.setOutputFile(TESTFILE);
      mediaRecorder.setMaxDuration(3000);

      // We wait a little and stop recording
      mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
        public void onInfo(MediaRecorder mr, int what, int extra) {
          Log.d(TAG,"MediaRecorder callback called !");
          if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.d(TAG,"MediaRecorder: MAX_DURATION_REACHED");
          } else if (what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            Log.d(TAG,"MediaRecorder: MAX_FILESIZE_REACHED");
          } else if (what==MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN) {
            Log.d(TAG,"MediaRecorder: INFO_UNKNOWN");
          } else {
            Log.d(TAG,"WTF ?");
          }
          mLock.release();
        }
      });

      // Start recording
      mediaRecorder.prepare();

      videoSurface = mMediaRecorder.getSurface();
      mediaRecorder.start();

      int flags      = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
      virtualDisplay = MEDIA_PROJECTION.createVirtualDisplay("ScreenCaster", mQuality.screenWidth, mQuality.screenHeight, mQuality.screenDpi, flags, videoSurface, /* callback= */ null, /* handler= */ null);

      if (mLock.tryAcquire(6,TimeUnit.SECONDS)) {
        Log.d(TAG,"MediaRecorder callback was called :)");
        Thread.sleep(400);
      } else {
        Log.d(TAG,"MediaRecorder callback was not called after 6 seconds... :(");
      }
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      try {
        mediaRecorder.stop();
      } catch (Exception e) {}
      mediaRecorder.release();
      mediaRecorder = null;

      virtualDisplay.release();
      virtualDisplay = null;

      videoSurface.release();
      videoSurface = null;
    }

    // Retrieve SPS & PPS & ProfileId with MP4Config
    MP4Config config = new MP4Config(TESTFILE);

    // Delete dummy video
    File file = new File(TESTFILE);
    if (!file.delete()) Log.e(TAG,"Temp file could not be erased");

    Log.i(TAG,"H264 Test succeded...");

    // Save test result
    if (mSettings != null) {
      Editor editor = mSettings.edit();
      editor.putString(key, config.getProfileLevel()+","+config.getB64SPS()+","+config.getB64PPS());
      editor.commit();
    }

    return config;
  }
}

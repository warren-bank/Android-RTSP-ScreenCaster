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
import android.util.Base64;
import android.util.Log;

import java.io.IOException;

/**
 * A class for streaming H.264 from the display screen of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setVideoQuality(VideoQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class H264Stream extends VideoStream {
  public final static String TAG = "H264Stream";

  private MP4Config mConfig;

  /**
   * Constructs the H.264 stream.
   * @throws IOException
   */
  public H264Stream() {
    super();
    mPacketizer = new H264Packetizer();
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
    return testMediaCodecAPI();
  }

  @SuppressLint("NewApi")
  private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
    try {
      EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.screenWidth, mQuality.screenHeight);
      return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
    } catch (Exception e) {
      Log.e(TAG,"Resolution not supported with the MediaCodec API.");
      return null;
    }
  }
}

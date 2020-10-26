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

/**
 * A class that represents the quality of a video stream.
 * It contains the resolution, the framerate (in fps) and the bitrate (in bps) of the stream.
 */
public class VideoQuality {
  public final static String TAG = "VideoQuality";

  /** Default video stream quality. */
  public static VideoQuality DEFAULT_VIDEO_QUALITY = null;

  public static void init(int screenWidth, int screenHeight, int screenDpi) {
    DEFAULT_VIDEO_QUALITY = new VideoQuality(screenWidth, screenHeight, screenDpi, /* framerate= */ 20, /* bitrate= */ 524288);
  }

  public int screenWidth  = 0;
  public int screenHeight = 0;
  public int screenDpi    = 0;
  public int framerate    = 0;
  public int bitrate      = 0;

  /**  Represents a quality for a video stream. */
  public VideoQuality() {}

  /**
   * Represents a quality for a video stream.
   * @param framerate The framerate in frame per seconds
   * @param bitrate The bitrate in bit per seconds
   */
  public VideoQuality(int screenWidth, int screenHeight, int screenDpi, int framerate, int bitrate) {
    this.screenWidth  = screenWidth;
    this.screenHeight = screenHeight;
    this.screenDpi    = screenDpi;
    this.framerate    = framerate;
    this.bitrate      = bitrate;
  }

  public boolean equals(VideoQuality quality) {
    if (quality==null) return false;
    return (
      (quality.screenWidth  == this.screenWidth)     &&
      (quality.screenHeight == this.screenHeight)    &&
      (quality.screenDpi    == this.screenDpi)       &&
      (quality.framerate    == this.framerate)       &&
      (quality.bitrate      == this.bitrate)
    );
  }

  public VideoQuality clone() {
    return new VideoQuality(screenWidth, screenHeight, screenDpi, framerate, bitrate);
  }

  public static VideoQuality extend(int framerate, int bitrate) {
    VideoQuality quality = null;

    if (DEFAULT_VIDEO_QUALITY == null)
      return quality;

    quality = DEFAULT_VIDEO_QUALITY.clone();
    quality.framerate = framerate;
    quality.bitrate   = bitrate;

    return quality;
  }

  public static VideoQuality parseQuality(String str) {
    VideoQuality quality = null;

    if (DEFAULT_VIDEO_QUALITY == null)
      return quality;

    quality = DEFAULT_VIDEO_QUALITY.clone();
    if (str == null)
      return quality;

    String[] config = str.split("-");
    try {
      quality.framerate = Integer.parseInt(config[0]);
      quality.bitrate   = Integer.parseInt(config[1]);
    }
    catch (IndexOutOfBoundsException ignore) {}

    return quality;
  }

  public String toString() {
    return screenWidth+"x"+screenHeight+" px, "+screenDpi+" dpi, "+framerate+" fps, "+(bitrate/1024/1024)+" Mbps";
  }
}

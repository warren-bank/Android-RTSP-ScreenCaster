package com.github.warren_bank.rtsp_screencaster.constant;

public class Constant {

  public interface ExtraDefaultValue {
    public static final int SERVER_PORT      =    6554;

    public static final int VIDEO_FRAMERATE  =      60;
    public static final int VIDEO_BITRATE    = 1048576;  // 1 Mbps

    public static final int AUDIO_SAMPLERATE =   44100;  // 44.1 kHz
    public static final int AUDIO_BITRATE    =   96000;  // 96 kbps
  }
}

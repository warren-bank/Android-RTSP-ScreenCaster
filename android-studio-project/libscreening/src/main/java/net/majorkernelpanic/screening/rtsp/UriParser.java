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

package net.majorkernelpanic.screening.rtsp;

import static net.majorkernelpanic.screening.SessionBuilder.AUDIO_AAC;
import static net.majorkernelpanic.screening.SessionBuilder.AUDIO_AMRNB;
import static net.majorkernelpanic.screening.SessionBuilder.AUDIO_NONE;
import static net.majorkernelpanic.screening.SessionBuilder.VIDEO_H263;
import static net.majorkernelpanic.screening.SessionBuilder.VIDEO_H264;
import static net.majorkernelpanic.screening.SessionBuilder.VIDEO_NONE;

import net.majorkernelpanic.screening.MediaStream;
import net.majorkernelpanic.screening.Session;
import net.majorkernelpanic.screening.SessionBuilder;
import net.majorkernelpanic.screening.audio.AudioQuality;
import net.majorkernelpanic.screening.video.VideoQuality;

import android.content.ContentValues;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * This class parses URIs received by the RTSP server and configures a Session accordingly.
 */
public class UriParser {
  public final static String TAG = "UriParser";

  /**
   * Configures a Session according to the given URI.
   * Here are some examples of URIs that can be used to configure a Session:
   * <ul>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554
   *   <ul>
   *     <li>video: h263, framerate=20 (20 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: none</li>
   *     <li>destination: IP of client</li>
   *     <li>ttl: 64 (64 network hops are permitted)</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?h264
   *   <ul>
   *     <li>video: h264, framerate=20 (20 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: none</li>
   *   </ul>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?h264=60
   *   <ul>
   *     <li>video: h264, framerate=60 (60 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: none</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?h264=60-1048576
   *   <ul>
   *     <li>video: h264, framerate=60 (60 fps), bitrate=1048576 (1 Mbps)</li>
   *     <li>audio: none</li>
   *   </ul>
   * </li>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?amr
   *   <ul>
   *     <li>video: h264, framerate=20 (20 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: amr, samplerate=8000 (8 kHz), bitrate=32000 (32 kbps)</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?aac
   *   <ul>
   *     <li>video: h264, framerate=20 (20 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: aac, samplerate=8000 (8 kHz), bitrate=32000 (32 kbps)</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?aac=44100-96000
   *   <ul>
   *     <li>video: h264, framerate=20 (20 fps), bitrate=524288 (.5 Mbps)</li>
   *     <li>audio: aac, samplerate=44100 (44.1 kHz), bitrate=96000 (96 kbps)</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?multicast
   *   <ul>
   *     <li>destination: 228.5.6.7</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?multicast=228.6.7.8
   *   <ul>
   *     <li>destination: 228.6.7.8</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?unicast=192.168.0.100
   *   <ul>
   *     <li>destination: 192.168.0.100</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?ttl=1
   *   <ul>
   *     <li>ttl: 1 (audio/video UTP data packets expire if they cannot reach destination within 1 network hop)</li>
   *   </ul>
   * </li>
   * <li>rtsp://xxx.xxx.xxx.xxx:8554?h264=60-1048576&aac=44100-96000&ttl=1</li>
   * </ul>
   * @param uri The URI
   * @throws IllegalStateException
   * @throws IOException
   * @return A Session configured according to the URI
   */
  public static Session parse(String uri) throws IllegalStateException, IOException {
    SessionBuilder builder     = SessionBuilder.getInstance().clone();
    byte           audioApi    = 0;
    String         query       = URI.create(uri).getQuery();
    String[]       queryParams = query == null ? new String[0] : query.split("&");
    ContentValues  params      = new ContentValues();

    for(String param:queryParams) {
      String[] keyValue = param.split("=");
      String value = "";
      try {
        value = keyValue[1];
      }catch(ArrayIndexOutOfBoundsException e){}

      params.put(
        URLEncoder.encode(keyValue[0], "UTF-8"), // Name
        URLEncoder.encode(value, "UTF-8")  // Value
      );
    }

    if (params.size()>0) {
      builder.setAudioEncoder(AUDIO_NONE).setVideoEncoder(VIDEO_NONE);
      Set<String> paramKeys=params.keySet();

      // Those parameters must be parsed first or else they won't necessarily be taken into account
      for(String paramName: paramKeys) {
        String paramValue = params.getAsString(paramName);

        // MULTICAST -> the stream will be sent to a multicast group
        // The default multicast address is 228.5.6.7, but the client can specify another
        if (paramName.equalsIgnoreCase("multicast")) {
          if (paramValue!=null) {
            try {
              InetAddress addr = InetAddress.getByName(paramValue);
              if (!addr.isMulticastAddress()) {
                throw new IllegalStateException("Invalid multicast address !");
              }
              builder.setDestination(paramValue);
            } catch (UnknownHostException e) {
              throw new IllegalStateException("Invalid multicast address !");
            }
          }
          else {
            // Default multicast address
            builder.setDestination("228.5.6.7");
          }
        }

        // UNICAST -> the client can use this to specify where he wants the stream to be sent
        else if (paramName.equalsIgnoreCase("unicast")) {
          if (paramValue!=null) {
            builder.setDestination(paramValue);
          }
        }

        // AUDIOAPI -> can be used to specify what api will be used to encode audio (the MediaRecorder API or the MediaCodec API)
        else if (paramName.equalsIgnoreCase("audioapi")) {
          if (paramValue!=null) {
            if (paramValue.equalsIgnoreCase("mr")) {
              audioApi = MediaStream.MODE_MEDIARECORDER_API;
            } else if (paramValue.equalsIgnoreCase("mc")) {
              audioApi = MediaStream.MODE_MEDIACODEC_API;
            }
          }
        }

        // TTL -> the client can modify the time to live of packets
        // By default ttl=64
        else if (paramName.equalsIgnoreCase("ttl")) {
          if (paramValue!=null) {
            try {
              int ttl = Integer.parseInt(paramValue);
              if (ttl<0) throw new IllegalStateException();
              builder.setTimeToLive(ttl);
            } catch (Exception e) {
              throw new IllegalStateException("The TTL must be a positive integer !");
            }
          }
        }

        // H.264
        else if (paramName.equalsIgnoreCase("h264")) {
          VideoQuality quality = VideoQuality.parseQuality(paramValue);
          builder.setVideoQuality(quality).setVideoEncoder(VIDEO_H264);
        }

        // H.263
        else if (paramName.equalsIgnoreCase("h263")) {
          VideoQuality quality = VideoQuality.parseQuality(paramValue);
          builder.setVideoQuality(quality).setVideoEncoder(VIDEO_H263);
        }

        // AMR
        else if (paramName.equalsIgnoreCase("amrnb") || paramName.equalsIgnoreCase("amr")) {
          AudioQuality quality = AudioQuality.parseQuality(paramValue);
          builder.setAudioQuality(quality).setAudioEncoder(AUDIO_AMRNB);
        }

        // AAC
        else if (paramName.equalsIgnoreCase("aac")) {
          AudioQuality quality = AudioQuality.parseQuality(paramValue);
          builder.setAudioQuality(quality).setAudioEncoder(AUDIO_AAC);
        }
      }
    }

    if (builder.getVideoEncoder()==VIDEO_NONE && builder.getAudioEncoder()==AUDIO_NONE) {
      SessionBuilder b = SessionBuilder.getInstance();
      builder.setVideoEncoder(b.getVideoEncoder());
      builder.setAudioEncoder(b.getAudioEncoder());
    }

    Session session = builder.build();

    if (audioApi>0 && session.getAudioTrack() != null) {
      session.getAudioTrack().setStreamingMethod(audioApi);
    }

    return session;
  }
}

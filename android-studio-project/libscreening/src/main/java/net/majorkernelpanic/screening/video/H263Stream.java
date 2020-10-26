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
import net.majorkernelpanic.screening.rtp.H263Packetizer;

import java.io.IOException;

/**
 * A class for streaming H.263 from the display screen of an android device using RTP.
 * You should use a {@link Session} instantiated with {@link SessionBuilder} instead of using this class directly.
 * Call {@link #setDestinationAddress(InetAddress)}, {@link #setDestinationPorts(int)} and {@link #setVideoQuality(VideoQuality)}
 * to configure the stream. You can then call {@link #start()} to start the RTP stream.
 * Call {@link #stop()} to stop the stream.
 */
public class H263Stream extends VideoStream {

  /**
   * Constructs the H.263 stream.
   * @throws IOException
   */
  public H263Stream() {
    super();
    mPacketizer = new H263Packetizer();
  }

  /**
   * Returns a description of the stream using SDP. It can then be included in an SDP file.
   */
  public String getSessionDescription() {
    return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
        "a=rtpmap:96 H263-1998/90000\r\n";
  }
}

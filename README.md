#### [RTSP ScreenCaster](https://github.com/warren-bank/Android-RTSP-ScreenCaster)

Android app that serves an RTSP video stream to mirror the device's display screen.

#### Overview:

* There is no UI when the app starts.
  - It's a foreground service with a notification, which runs an RTSP server on port 6554.
  - The URL to access the video stream from a client is given in the notification message.

#### Limitations:

* Android doesn't support system audio capture
  - Bluetooth works nicely (in combination)

#### Requirements:

* Android 5.0 (API Level 21) or higher
  - [MediaProjectionManager](https://developer.android.com/reference/android/media/projection/MediaProjectionManager)

#### Credits:

* [libstreaming](https://github.com/fyhertz/libstreaming) library
  - author/copyright:
    * [Simon Guigui](https://github.com/fyhertz)
  - license:
    * [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) after commit [71558f6](https://github.com/fyhertz/libstreaming/commit/71558f6c587bbcc115718292df332d05d13651d0) on Mar 13, 2017, 3:47 PM PDT
    * [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.txt) prior
  - the `libscreening` library is forked from:
    * commit [f620117](https://github.com/fyhertz/libstreaming/tree/f6201177b4669bb9fe50dac9632510a8ad75ad7b) on Mar 13, 2017, 9:34 AM PDT
    * GPL-3.0 license

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.txt)

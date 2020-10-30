/*
 * ScreenRecordingSample
 * Sample project to cature and save audio from internal and video from screen as MPEG4 file.
 *
 * Copyright (c) 2015-2016 saki t_saki@serenegiant.com
 *
 * File name: MediaScreenEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

/*
 * =======
 * Origin:
 * =======
 *  https://github.com/saki4510t/ScreenRecordingSample
 *  https://github.com/saki4510t/ScreenRecordingSample/blob/9419559a193f2b90c8f86de82c975494a7b2f7d0/app/src/main/java/com/serenegiant/media/MediaScreenEncoder.java#L112
 *
 * =============
 * Dependencies:
 * =============
 *  https://github.com/saki4510t/libcommon/blob/master/common/src/main/java/com/serenegiant/glutils/EglTask.java
 *  https://github.com/saki4510t/libcommon/blob/master/common/src/main/java/com/serenegiant/utils/MessageTask.java
*/

package net.majorkernelpanic.screening.video;

import com.serenegiant.glutils.EGLBase;
import com.serenegiant.glutils.EglTask;
import com.serenegiant.glutils.GLDrawer2D;

import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

public final class DrawTask extends EglTask {
  private static final boolean DEBUG = true;
  private static final String  TAG   = "VideoStream:DrawTask";

  private final Object        mSync      = new Object();
  private final float[]       mTexMatrix = new float[16];

  private MediaProjection     mMediaProjection;
  private Surface             mSurface;
  private VideoQuality        mQuality;
  private Handler             mHandler;

  private GLDrawer2D          mDrawer;
  private int                 mTexId;
  private SurfaceTexture      mSourceTexture;
  private Surface             mSourceSurface;
  private EGLBase.IEglSurface mEncoderSurface;
  private long                intervals;
  private VirtualDisplay      display;
  private boolean             mIsRecording;
  private boolean             requestDraw;

  // Callback listener when receiving the video at TextureSurface
  private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
    @Override
    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
    //if (DEBUG) Log.v(TAG, "SurfaceTexture.Callback#onFrameAvailable: mIsRecording=" + mIsRecording);
      if (mIsRecording) {
        synchronized (mSync) {
          requestDraw = true;
          mSync.notifyAll();
        }
      }
    }
  };

  private final VirtualDisplay.Callback mCallback = (!DEBUG) ? null : new VirtualDisplay.Callback() {
    /**
     * Called when the virtual display video projection has been
     * paused by the system or when the surface has been detached
     * by the application by calling setSurface(null).
     * The surface will not receive any more buffers while paused.
     */
    @Override
    public void onPaused() {
      if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onPaused");
    }

    /**
     * Called when the virtual display video projection has been
     * resumed after having been paused.
     */
    @Override
    public void onResumed() {
      if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onResumed");
    }

    /**
     * Called when the virtual display video projection has been
     * stopped by the system.  It will no longer receive frames
     * and it will never be resumed.  It is still the responsibility
     * of the application to release() the virtual display.
     */
    @Override
    public void onStopped() {
      if (DEBUG) Log.v(TAG, "VirtualDisplay.Callback#onStopped");
    }
  };

  private final Runnable mDrawTask = new Runnable() {
    @Override
    public void run() {
    //if (DEBUG) Log.v(TAG, "draw");
      boolean local_request_draw;
      synchronized (mSync) {
        local_request_draw = requestDraw;
        if (!requestDraw) {
          try {
            mSync.wait(intervals);
            local_request_draw = requestDraw;
            requestDraw = false;
          } catch (final InterruptedException e) {
            return;
          }
        }
      }
      if (mIsRecording) {
        if (local_request_draw) {
          mSourceTexture.updateTexImage();
          mSourceTexture.getTransformMatrix(mTexMatrix);
        }

        // Draw the image received by SurfaceTexture on the input Surface of MediaCodec
        mEncoderSurface.makeCurrent();
        mDrawer.draw(mTexId, mTexMatrix, 0);
        mEncoderSurface.swap();

        // Workaround for models that hang if not drawn off-screen for EGL retention
        makeCurrent();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glFlush();
        queueEvent(this);
      } else {
        releaseSelf();
      }
    //if (DEBUG) Log.v(TAG, "draw:finished");
    }
  };

  public DrawTask(
    final EGLBase.IContext sharedContext,
    final int flags,
    final MediaProjection mediaProjection,
    final Surface surface,
    final VideoQuality quality
  ) {
    super(sharedContext, flags);

    this.mMediaProjection = mediaProjection;
    this.mSurface         = surface;
    this.mQuality         = quality;

    final HandlerThread thread = new HandlerThread(TAG);
    thread.start();
    mHandler = new Handler(thread.getLooper());
  }

  @Override
  protected void onStart() {
    if (DEBUG) Log.d(TAG, "onStart");

    mDrawer = new GLDrawer2D(true);
    mTexId = mDrawer.initTex();
    mSourceTexture = new SurfaceTexture(mTexId);
    mSourceTexture.setDefaultBufferSize(mQuality.screenWidth, mQuality.screenHeight);  // If you don't put this in, you can't get the image
    mSourceSurface = new Surface(mSourceTexture);
    mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
    mEncoderSurface = getEgl().createFromSurface(mSurface);

    if (DEBUG) Log.d(TAG, "setup VirtualDisplay");
    intervals = (long)(1000f / mQuality.framerate);
    display = mMediaProjection.createVirtualDisplay(
      "RTSP ScreenCaster",
      mQuality.screenWidth, mQuality.screenHeight, mQuality.screenDpi,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      mSourceSurface, mCallback, mHandler
    );
    if (DEBUG) Log.v(TAG, "screen capture loop: display=" + display);

    mIsRecording = true;
    requestDraw  = false;

    queueEvent(mDrawTask);
  }

  private void releaseAllResources() {
    mIsRecording = false;
    requestDraw  = false;

    if (display != null) {
      if (DEBUG) Log.d(TAG, "release VirtualDisplay");
      display.release();
      display = null;
    }
    if (mEncoderSurface != null) {
      mEncoderSurface.release();
      mEncoderSurface = null;
    }
    if (mSourceSurface != null) {
      mSourceSurface.release();
      mSourceSurface = null;
    }
    if (mSourceTexture != null) {
      mSourceTexture.release();
      mSourceTexture = null;
    }
    if (mDrawer != null) {
      mDrawer.release();
      mDrawer = null;
    }

    try {
      makeCurrent();
    }
    catch (final Exception e) {}
  }

  @Override
  protected void onStop() {
    if (DEBUG) Log.d(TAG, "onStop");
    releaseAllResources();
  }

  @Override
  protected void onRelease() {
    super.onRelease();
    if (DEBUG) Log.d(TAG, "onRelease");
    releaseAllResources();
  }

  @Override
  protected boolean onError(final Exception e) {
    super.onError(e);
    if (DEBUG) Log.w(TAG, e);

    return false;
  }

  @Override
  protected Object processRequest(final int request, final int arg1, final int arg2, final Object obj) {
    return null;
  }

  @Override
  public void release() {
    releaseAllResources();

    super.release(true);
  }
}

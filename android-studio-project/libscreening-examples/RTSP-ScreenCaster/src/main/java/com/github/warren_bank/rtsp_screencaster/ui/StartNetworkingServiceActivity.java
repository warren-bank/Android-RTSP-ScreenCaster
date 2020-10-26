package com.github.warren_bank.rtsp_screencaster.ui;

import net.majorkernelpanic.screening.SessionBuilder;

import com.github.warren_bank.rtsp_screencaster.R;
import com.github.warren_bank.rtsp_screencaster.MainApp;
import com.github.warren_bank.rtsp_screencaster.constant.Constant;
import com.github.warren_bank.rtsp_screencaster.service.NetworkingService;
import com.github.warren_bank.rtsp_screencaster.utils.RuntimePermissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class StartNetworkingServiceActivity extends Activity {
  private static final String STATE_RESULT_CODE = "result_code";
  private static final String STATE_RESULT_DATA = "result_data";

  private static final int REQUEST_CODE_MEDIA_PROJECTION    = 1;
  private static final int REQUEST_CODE_RUNTIME_PERMISSIONS = 2;

  private MediaProjectionManager mediaProjectionManager;
  private int                    mediaProjectionResultCode;
  private Intent                 mediaProjectionResultData;

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (mediaProjectionResultData != null) {
      outState.putInt(STATE_RESULT_CODE,        mediaProjectionResultCode);
      outState.putParcelable(STATE_RESULT_DATA, mediaProjectionResultData);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_start_networking_service);

    mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    if(savedInstanceState != null) {
      mediaProjectionResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
      mediaProjectionResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
    }

    initMediaProjection();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
      if ((resultCode == Activity.RESULT_OK) && (resultData != null)) {
        mediaProjectionResultCode = resultCode;
        mediaProjectionResultData = resultData;
      }

      initMediaProjection();
    }
  }

  private void initMediaProjection() {
    if (mediaProjectionResultData != null) {
      initNetworkingSession();
    }
    else {
      startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
    }
  }

  private void initNetworkingSession() {
    Context           context = getApplicationContext();
    SharedPreferences prefs   = PreferenceManager.getDefaultSharedPreferences(context);

    /* update RTSP Server settings */
    Editor editor = prefs.edit();
    editor.putBoolean(NetworkingService.KEY_ENABLED, true);
    editor.putString( NetworkingService.KEY_PORT,    String.valueOf(Constant.ExtraDefaultValue.SERVER_PORT));
    editor.commit();

    /* update SessionBuilder singleton */
    SessionBuilder.getInstance()
      .setContext(context)
      .setMediaProjection(mediaProjectionResultCode, mediaProjectionResultData)
      .setAudioEncoder(SessionBuilder.AUDIO_AAC)
      .setVideoEncoder(SessionBuilder.VIDEO_H264)
      .setVideoQuality(Constant.ExtraDefaultValue.VIDEO_FRAMERATE,  Constant.ExtraDefaultValue.VIDEO_BITRATE)
      .setAudioQuality(Constant.ExtraDefaultValue.AUDIO_SAMPLERATE, Constant.ExtraDefaultValue.AUDIO_BITRATE)
    ;

    RuntimePermissions.REQUEST_CODE = REQUEST_CODE_RUNTIME_PERMISSIONS;

    if (RuntimePermissions.isEnabled(StartNetworkingServiceActivity.this))
      startNetworkingService();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    RuntimePermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, new RuntimePermissions.Callback() {
      @Override
      public void onPermissionsCancelled() {
        if (RuntimePermissions.isEnabled(StartNetworkingServiceActivity.this))
          startNetworkingService();
      }

      @Override
      public void onPermissionsGranted() {
        startNetworkingService();
      }

      @Override
      public void onPermissionsDenied(String[] permissions) {
        if (RuntimePermissions.isEnabled(StartNetworkingServiceActivity.this))
          startNetworkingService();
      }
    });
  }

  private void startNetworkingService() {
    Context context = getApplicationContext();

    /* start RTSP Server in foreground service */
    Intent intent = new Intent(context, NetworkingService.class);
    MainApp.getInstance().startService(intent);

    /* close */
    finish();
  }
}

package com.github.warren_bank.rtsp_screencaster.utils;

import net.majorkernelpanic.screening.SessionBuilder;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.Arrays;

public final class RuntimePermissions {

  public static interface Callback {
    public void onPermissionsCancelled();
    public void onPermissionsGranted();
    public void onPermissionsDenied(String[] permissions);
  }

  public static int REQUEST_CODE = 1;

  private static ArrayList<String> REQUIRED_PERMISSIONS = null;

  public static String[] getMissingPermissions(Context context) {
    return getMissingPermissions(context, null);
  }

  public static String[] getMissingPermissions(Context context, SessionBuilder builder) {
    if (Build.VERSION.SDK_INT < 23)
      return new String[0];

    REQUIRED_PERMISSIONS = new ArrayList<String>(
      Arrays.asList(
        SessionBuilder.getRequiredPermissions(builder)
      )
    );

    if (REQUIRED_PERMISSIONS.isEmpty())
      return new String[0];

    ArrayList<String> missingPermissions = new ArrayList<>();
    PackageManager    pkgMngr = context.getPackageManager();
    String            pkgName = context.getPackageName();

    for (String permission : REQUIRED_PERMISSIONS) {
      if (pkgMngr.checkPermission(permission, pkgName) != PackageManager.PERMISSION_GRANTED)
        missingPermissions.add(permission);
    }

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  public static boolean isEnabled(Activity activity) {
    if (Build.VERSION.SDK_INT < 23)
      return true;

    String[] missingPermissions = getMissingPermissions(activity);

    if (missingPermissions.length == 0)
      return true;

    activity.requestPermissions(missingPermissions, REQUEST_CODE);
    return false;
  }

  public static void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults, Callback cb) {
    if (
         (requestCode != REQUEST_CODE)
      || (null == cb)
      || (null == REQUIRED_PERMISSIONS)
    ) {
      return;
    }

    if (grantResults.length == 0) {
      if (permissions.length == 0) {
        // no "dangerous" permissions are needed
        cb.onPermissionsGranted();
      }
      else {
        // request was cancelled. show the prompts again.
        cb.onPermissionsCancelled();
      }
    }
    else {
      ArrayList<String> deniedPermissions = new ArrayList<>();

      for (int i=0; i < grantResults.length; i++) {
        if (
          (grantResults[i] != PackageManager.PERMISSION_GRANTED) &&
          REQUIRED_PERMISSIONS.contains(permissions[i])
        ) {
          // a mandatory permission is not granted
          deniedPermissions.add(permissions[i]);
        }
      }

      if (deniedPermissions.isEmpty()) {
        cb.onPermissionsGranted();
      }
      else {
        cb.onPermissionsDenied(
          deniedPermissions.toArray(new String[deniedPermissions.size()])
        );
      }
    }
  }
}

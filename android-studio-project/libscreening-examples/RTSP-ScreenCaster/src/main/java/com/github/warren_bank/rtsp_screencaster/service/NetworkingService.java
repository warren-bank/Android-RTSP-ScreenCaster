package com.github.warren_bank.rtsp_screencaster.service;

import net.majorkernelpanic.screening.rtsp.RtspServer;

import com.github.warren_bank.rtsp_screencaster.R;
import com.github.warren_bank.rtsp_screencaster.utils.NetworkUtils;
import com.github.warren_bank.rtsp_screencaster.utils.ResourceUtils;
import com.github.warren_bank.rtsp_screencaster.utils.WakeLockMgr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import java.net.InetAddress;

public class NetworkingService extends RtspServer {
  private static final String ACTION_STOP = "STOP";

  private static NetworkingService instance;

  private InetAddress localAddress;

  @Override
  public void onCreate() {
    /** The server name that will appear in responses. */
    RtspServer.SERVER_NAME = "ScreenCaster RTSP Server";

    super.onCreate();

    instance = NetworkingService.this;
    WakeLockMgr.acquire(/* context= */ instance);
    showNotification();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    onStart(intent, startId);
    return START_STICKY;
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);

    processIntent(intent);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    instance = null;
    WakeLockMgr.release();
    hideNotification();
  }

  public static NetworkingService getInstance() {
    return instance;
  }

  public static void stopInstance() {
    if (instance != null) {
      instance.stopSelf();
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  // -------------------------------------------------------------------------
  // foregrounding..

  private String getNotificationChannelId() {
    return getPackageName();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= 26) {
      String channelId       = getNotificationChannelId();
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      NotificationChannel NC = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);

      NC.setDescription(channelId);
      NC.setSound(null, null);
      NM.createNotificationChannel(NC);
    }
  }

  private int getNotificationId() {
    return ResourceUtils.getInteger(instance, R.integer.NOTIFICATION_ID_NETWORKING_SERVICE);
  }

  private void showNotification() {
    Notification notification = getNotification();
    int NOTIFICATION_ID = getNotificationId();

    if (Build.VERSION.SDK_INT >= 5) {
      createNotificationChannel();
      startForeground(NOTIFICATION_ID, notification);
    }
    else {
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      NM.notify(NOTIFICATION_ID, notification);
    }
  }

  private void hideNotification() {
    if (Build.VERSION.SDK_INT >= 5) {
      stopForeground(true);
    }
    else {
      NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      int NOTIFICATION_ID    = getNotificationId();
      NM.cancel(NOTIFICATION_ID);
    }
  }

  private Notification getNotification() {
    Notification notification  = (Build.VERSION.SDK_INT >= 26)
      ? (new Notification.Builder(/* context= */ instance, /* channelId= */ getNotificationChannelId())).build()
      :  new Notification()
    ;

    notification.when          = System.currentTimeMillis();
    notification.flags         = 0;
    notification.flags        |= Notification.FLAG_ONGOING_EVENT;
    notification.flags        |= Notification.FLAG_NO_CLEAR;
    notification.icon          = R.drawable.launcher;
    notification.tickerText    = getString(R.string.notification_service_ticker);
    notification.contentIntent = getPendingIntent_StopService();
    notification.deleteIntent  = getPendingIntent_StopService();

    if (Build.VERSION.SDK_INT >= 16) {
      notification.priority    = Notification.PRIORITY_HIGH;
    }
    else {
      notification.flags      |= Notification.FLAG_HIGH_PRIORITY;
    }

    if (Build.VERSION.SDK_INT >= 21) {
      notification.visibility  = Notification.VISIBILITY_PUBLIC;
    }

    RemoteViews contentView    = new RemoteViews(getPackageName(), R.layout.service_notification);
    contentView.setImageViewResource(R.id.notification_icon, R.drawable.launcher);
    contentView.setTextViewText(R.id.notification_text_line1, getString(R.string.notification_service_content_line1));
    contentView.setTextViewText(R.id.notification_text_line2, getNetworkAddress());
    contentView.setTextViewText(R.id.notification_text_line3, getString(R.string.notification_service_content_line3));
    notification.contentView   = contentView;

    return notification;
  }

  private PendingIntent getPendingIntent_StopService() {
    Intent intent = new Intent(instance, NetworkingService.class);
    intent.setAction(ACTION_STOP);

    return PendingIntent.getService(instance, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  private String getNetworkAddress() {
    if (localAddress == null)
      localAddress = NetworkUtils.getLocalIpAddress(); //Get local IP object

    return (localAddress == null)
      ? "[offline]"
      : "rtsp://" + localAddress.getHostAddress() + ":" + getPort();
  }

  // -------------------------------------------------------------------------
  // process inbound intents

  private void processIntent(Intent intent) {
    if (intent == null) return;

    String action = intent.getAction();

    if ((action != null) && action.equals(ACTION_STOP))
      instance.stopSelf();
  }
}

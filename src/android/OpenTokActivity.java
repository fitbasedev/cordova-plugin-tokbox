package com.fitbase.TokBox;


import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.ViewGroup;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import android.content.res.Configuration;

import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;



import com.fitbase.MainActivity;
import com.fitbase.R;





public class OpenTokActivity extends AppCompatActivity
  implements EasyPermissions.PermissionCallbacks,
  Publisher.PublisherListener,
  Session.SessionListener,Session.ReconnectionListener ,Subscriber.VideoListener,Session.SignalListener,Session.ConnectionListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  public static final String SIGNAL_TYPE = "closeConnection";


  private static final int RC_SETTINGS_SCREEN_PERM = 123;
  private static final int RC_VIDEO_APP_PERM = 124;

  private Session mSession;
  private Publisher mPublisher;
  String key;
  private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
  private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();
  private RelativeLayout mPublisherViewContainer;
  long time;
  RelativeLayout mSubscriberViewContainer,subscriberAudio;
  private ImageView mLocalAudioOnlyImage,avatar;
  Set<String> connectionMetaData = new HashSet<String>();
  private String tokBoxData, apiKey, token, sessionId, publisherId, duration, startdate,logedInUserId;
  private Handler hidehandler;
  private static final String FORMAT_2 = "%02d";
  ImageButton btnPausevideo, btnPauseaudio, btn_exit;
  LinearLayout llcontrols;
  private TextView tvtimer,init_info,  mAlert;
  private ImageButton remoteAudio;
  private ProgressDialog mProgressDialog,mSessionReconnectDialog;
  public boolean isWantToContinueHere;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisher_container);
    mPublisherViewContainer.setVisibility(View.INVISIBLE);
    mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview0);

    btnPausevideo = (ImageButton) findViewById(R.id.btn_pausevideo);
    btnPauseaudio = (ImageButton) findViewById(R.id.btn_pauseaudio);
    btn_exit = (ImageButton) findViewById(R.id.btn_exit);
    llcontrols = (LinearLayout) findViewById(R.id.llcontrols);
    tvtimer = (TextView) findViewById(R.id.tvtimer);
    init_info=(TextView)findViewById(R.id.init_info);
    mPublisherViewContainer.setOnTouchListener(new OnDragTouchListener(mPublisherViewContainer));
    mAlert = (TextView) findViewById(R.id.quality_warning);
    mSessionReconnectDialog = new ProgressDialog(OpenTokActivity.this);
    //-------------------audio 0-------------------------------
    subscriberAudio=(RelativeLayout)findViewById(R.id.remoteControls);
    remoteAudio=(ImageButton)findViewById(R.id.remoteAudio0);
    //--------------------------------------------------------
    hidehandler = new Handler();
    tokBoxData = getIntent().getStringExtra("tokbox_obj");
    try {
      JSONObject jobj = new JSONObject(tokBoxData);
      apiKey = jobj.getString("apiKey");
      token = jobj.getString("tokenId");
      sessionId = jobj.getString("liveSessionId");
      publisherId = jobj.getString("trainerUserid");
      duration = jobj.getString("duration");
      startdate = jobj.getString("startDate");
      logedInUserId=jobj.getString("logedInUserId");
    } catch (Exception e) {
      e.printStackTrace();
    }
    btnPausevideo.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mPublisher.getPublishVideo()) {
          mPublisher.setPublishVideo(false);
          onDisableLocalVideo(false);
          btnPausevideo.setImageResource(R.drawable.no_video_icon);
        } else {
          mPublisher.setPublishVideo(true);
          onDisableLocalVideo(true);
          btnPausevideo.setImageResource( R.drawable.video_icon);
        }
      }
    });
    btnPauseaudio.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mPublisher.getPublishAudio()) {
          mPublisher.setPublishAudio(false);

          btnPauseaudio.setImageResource(R.drawable.muted_mic_icon);
        } else {
          mPublisher.setPublishAudio(true);

          btnPauseaudio.setImageResource(R.drawable.mic_icon);
        }

      }
    });
    btn_exit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        onBackPressed();
      }
    });

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
      sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // missing line
      Date date = sdf.parse(startdate.split("\\.")[0]);
      SimpleDateFormat writeDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.ENGLISH);
      writeDate.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
      String s = writeDate.format(date);
      Date date1 = writeDate.parse(s);
      Calendar c = Calendar.getInstance();
      long millis=(date1.getTime()+(Long.parseLong(duration)*60*1000))- c.getTimeInMillis();

      new CountDownTimer(millis, 1000) { // adjust the milli seconds here

        public void onTick(long millisUntilFinished) {
          time=millisUntilFinished;
          tvtimer.setText("" + TimeUnit.MILLISECONDS.toHours(millisUntilFinished) + " : " + String.format(FORMAT_2, TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished))) + " : " + String.format(FORMAT_2, TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
        }

        public void onFinish() {

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (mSession != null) {
                mSession.onPause();
                if (isFinishing()) {
                  disconnectSession();
                }
              }
              if (hidehandler != null && hideControllerThread != null)
                hidehandler.removeCallbacks(hideControllerThread);
              finish();
            }
          });

        }

      }.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
    requestPermissions();
  }
  public void onDisableLocalVideo(boolean video) {
    if (!video) {

      mLocalAudioOnlyImage = new ImageView(this);
      mLocalAudioOnlyImage.setImageResource(R.mipmap.avatar);
      mLocalAudioOnlyImage.setBackgroundResource(R.drawable.bckg_audio_only);
      mPublisherViewContainer.addView(mLocalAudioOnlyImage);
    } else {
      mPublisherViewContainer.removeView(mLocalAudioOnlyImage);
    }


  }
  public void onDisableRemoteVideo(boolean video){
    if (!video) {
      RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
      avatar = new ImageView(this);
      avatar.setImageResource(R.mipmap.avatar);
      avatar.setBackgroundResource(R.drawable.bckg_audio_only);
      mSubscriberViewContainer.addView(avatar,layoutParams);
      subscriberAudio.bringToFront();
    } else {
      mSubscriberViewContainer.removeView(avatar);
    }
  }
  @Override
  protected void onStart() {
    Log.d(TAG, "onStart");

    super.onStart();
  }
  public void swapCamera(View view) {

    mPublisher.cycleCamera();

  }
  private Runnable hideControllerThread = new Runnable() {

    public void run() {
      llcontrols.setVisibility(View.GONE);
    }
  };


  public void hideControllers() {

    hidehandler.postDelayed(hideControllerThread, 10000);
  }

  public void showControllers() {
    llcontrols.setVisibility(View.VISIBLE);
    hidehandler.removeCallbacks(hideControllerThread);
    hideControllers();
  }

  @Override
  public void onUserInteraction() {
    super.onUserInteraction();

    if (llcontrols.getVisibility() == View.VISIBLE) {
      hidehandler.removeCallbacks(hideControllerThread);
      hideControllers();
    } else {
      showControllers();
    }
  }


  @Override
  protected void onRestart() {
    Log.d(TAG, "onRestart");

    super.onRestart();
  }

  @Override
  protected void onResume() {
    Log.d(TAG, "onResume");

    super.onResume();

    if (mSession == null) {
      return;
    }
    mSession.onResume();
    hideControllers();
  }

  @Override
  protected void onPause() {
    Log.d(TAG, "onPause");

    super.onPause();

    if (mSession == null) {
      return;
    }
    mSession.onPause();

    if (isFinishing()) {
      disconnectSession();
    }
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "onPause");

    super.onStop();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "onDestroy");

    disconnectSession();

    super.onDestroy();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }

  @Override
  public void onPermissionsGranted(int requestCode, List<String> perms) {
    Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
  }

  @Override
  public void onPermissionsDenied(int requestCode, List<String> perms) {
    Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

    if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
      new AppSettingsDialog.Builder(this)
        .setTitle(getString(R.string.title_settings_dialog))
        .setRationale(getString(R.string.rationale_ask_again))
        .setPositiveButton(getString(R.string.setting))
        .setNegativeButton(getString(R.string.cancel))
        .setRequestCode(RC_SETTINGS_SCREEN_PERM)
        .build()
        .show();
    }
  }

  @AfterPermissionGranted(RC_VIDEO_APP_PERM)
  private void requestPermissions() {
    String[] perms = {
      Manifest.permission.INTERNET,
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO
    };
    if (EasyPermissions.hasPermissions(this, perms)) {
      mSession = new Session.Builder(OpenTokActivity.this, apiKey, sessionId).build();
      mSession.setSessionListener(this);
      mProgressDialog = new ProgressDialog(this);
      mProgressDialog.setCanceledOnTouchOutside(false);
      mProgressDialog.setTitle("Please wait");
      mProgressDialog.setMessage("Connecting...");
      mProgressDialog.show();
      mSession.connect(token);
    } else {
      EasyPermissions.requestPermissions(OpenTokActivity.this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
    }
  }
  @Override
  public void onConnected(Session session) {
    Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
    mProgressDialog.dismiss();
    mPublisher = new Publisher.Builder(OpenTokActivity.this).name("publisher").build();
    //mPublisher.setRenderer(new BasicCustomVideoRenderer(this));
      mSession.setReconnectionListener(this);
      mSession.setSessionListener(this);
      mSession.setSignalListener(this);
      mSession.setConnectionListener(this);
    mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    mPublisherViewContainer.addView(mPublisher.getView());
    mPublisherViewContainer.setVisibility(View.VISIBLE);
    init_info.setBackgroundResource(R.color.quality_warning);
    init_info.setTextColor(this.getResources().getColor(R.color.white));
    init_info.bringToFront();
    init_info.setVisibility(View.VISIBLE);
    key=session.getConnection().getData();
    connectionMetaData.add(key);
    mSession.publish(mPublisher);
  }

  @Override
  public void onDisconnected(Session session) {
    Log.d(TAG, "onDisconnected: disconnected from session " + session.getSessionId());

    mSession = null;
  }

  @Override
  public void onError(Session session, OpentokError opentokError) {
    Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in session " + session.getSessionId());
    mProgressDialog.dismiss();
    Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
    finish();
  }

  @Override
  public void onStreamReceived(Session session, Stream stream) {
    Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());
    String publisherId = stream.getConnection().getData();
   if (this.publisherId.equalsIgnoreCase(publisherId)) {
      init_info.setVisibility(View.INVISIBLE);
      final Subscriber subscriber = new Subscriber.Builder(OpenTokActivity.this, stream).build();
      subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
      subscriber.setVideoListener(this);
      mSession.subscribe(subscriber);
      mSubscribers.add(subscriber);
      mSubscriberStreams.put(stream, subscriber);
      calculateLayout();
   }

  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    DisplayMetrics maMetrics  =getDisplay();
    if (mSubscribers.size() == 1) {
      mSubscriberViewContainer.getLayoutParams().height = maMetrics.heightPixels;
      mSubscriberViewContainer.getLayoutParams().width = maMetrics.widthPixels;
      mSubscriberViewContainer.requestLayout();
    }

  }

  private void calculateLayout( ) {
    DisplayMetrics maMetrics  =getDisplay();
    if(mSubscribers.size()==1){
      boolean isMuted=mSubscribers.get(0).getSubscribeToAudio();
      mPublisherViewContainer.setVisibility(View.VISIBLE);
      mSubscriberViewContainer.addView(mSubscribers.get(0).getView());
      subscriberAudio.setVisibility(View.VISIBLE);
      subscriberAudio.bringToFront();;
      remoteAudio.setOnClickListener(clickListener);
      remoteAudio.setTag(mSubscribers.get(0).getStream());
      remoteAudio.setImageResource(isMuted ? R.drawable.audio : R.drawable.no_audio);
      mSubscriberViewContainer.getLayoutParams().height=maMetrics.heightPixels;
      mSubscriberViewContainer.getLayoutParams().width=maMetrics.widthPixels;
      mSubscriberViewContainer.requestLayout();

    }
  }

  public DisplayMetrics getDisplay(){
    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    return metrics;
  }
  private View.OnClickListener clickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      Subscriber participant = mSubscriberStreams.get(view.getTag());
      boolean enableAudioOnly = participant.getSubscribeToAudio();
      if (enableAudioOnly) {
        participant.setSubscribeToAudio(false);
        ((ImageButton)view).setImageResource(R.drawable.no_audio);
      } else {
        participant.setSubscribeToAudio(true);
        ((ImageButton)view).setImageResource(R.drawable.audio);
      }
    }
  };

  @Override
  public void onStreamDropped(Session session, Stream stream) {
    Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

    Subscriber subscriber = mSubscriberStreams.get(stream);
    if (subscriber == null) {
      return;
    }
    connectionMetaData.remove(stream.getConnection().getData());
    mSubscribers.remove(subscriber);
    mSubscriberStreams.remove(stream);

    mSubscriberViewContainer.removeView(subscriber.getView());
    mSubscriberViewContainer.removeView(avatar);
    init_info.setBackgroundResource(R.color.quality_warning);
    init_info.setTextColor(OpenTokActivity.this.getResources().getColor(R.color.white));
    init_info.bringToFront();
    init_info.setVisibility(View.VISIBLE);
    subscriberAudio.setVisibility(View.INVISIBLE);
  }



  @Override
  public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
    Log.d(TAG, "onStreamCreated: Own stream " + stream.getStreamId() + " created");
  }

  @Override
  public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
    Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");
  }

  @Override
  public void onError(PublisherKit publisherKit, OpentokError opentokError) {
    Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

    Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
    finish();
  }

  private void disconnectSession() {
    if (mSession == null) {
      return;
    }

    if (mSubscribers.size() > 0) {
      for (Subscriber subscriber : mSubscribers) {
        if (subscriber != null) {
          mSession.unsubscribe(subscriber);
          subscriber.destroy();
          mSubscribers.remove(subscriber);
        }
      }
    }

    if (mPublisher != null) {
      mPublisherViewContainer.removeView(mPublisher.getView());
      mSession.unpublish(mPublisher);
      mPublisher.destroy();
      mPublisher = null;
    }
    mSession.disconnect();
  }

  @Override
  public void onReconnecting(Session session) {
    showReconnectionDialog(true);
  }

  @Override
  public void onReconnected(Session session) {
    showReconnectionDialog(false);
  }

  @Override
  public void onVideoDataReceived(SubscriberKit subscriberKit) {

  }

  @Override
  public void onVideoDisabled(SubscriberKit subscriberKit, String reason) {
    if (reason.equals("quality")) {
      showNetworkWarning();
    }else if(reason.equals("publishVideo")) {
      onDisableRemoteVideo(false);
    }
  }

  @Override
  public void onVideoEnabled(SubscriberKit subscriberKit, String s) {
    onDisableRemoteVideo(true);
  }





  @Override
  public void onVideoDisableWarning(SubscriberKit subscriberKit) {

  }

  @Override
  public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

  }
  private void showReconnectionDialog(boolean show) {
    if (show) {
      mSessionReconnectDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mSessionReconnectDialog.setMessage("Reconnecting. Please wait...");
      mSessionReconnectDialog.setIndeterminate(true);
      mSessionReconnectDialog.setCanceledOnTouchOutside(false);
      mSessionReconnectDialog.show();
    }
    else {
      mSessionReconnectDialog.dismiss();
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage("Session has been reconnected")
        .setPositiveButton(android.R.string.ok, null);
      builder.create();
      builder.show();
    }
  }
  public void showNetworkWarning(){
    mAlert.setBackgroundResource(R.color.quality_warning);
    mAlert.setTextColor(this.getResources().getColor(R.color.white));
    mAlert.bringToFront();
    mAlert.setVisibility(View.VISIBLE);
    mAlert.postDelayed(new Runnable() {
      public void run() {
        mAlert.setVisibility(View.GONE);
      }
    }, 7000);
  }

  @Override
  public void onSignalReceived(Session session, String type, String data, Connection connection) {
    String myConnectionId = session.getConnection().getConnectionId();
    if (!connection.getConnectionId().equals(myConnectionId) && type != null && type.equals(SIGNAL_TYPE) && data.equals(logedInUserId)) {
      onBackPressed();
    }
  }

  @Override
  public void onConnectionCreated(Session session, Connection connection) {
    key=connection.getData();
    if(connectionMetaData.contains(key)){
      if(mSession.getConnection().getConnectionId()==session.getConnection().getConnectionId()){
        isWantToContinueHere=false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        builder.setMessage("Looks like your stream is already running ! Do you want it to start here..?").setPositiveButton("Yes", dialogClickListener)
          .setNegativeButton("No", dialogClickListener).show();
      }
    }else{
      connectionMetaData.add(key);
    }
  }
  DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
      switch (which){
        case DialogInterface.BUTTON_POSITIVE:
          mSession.sendSignal(SIGNAL_TYPE, key);
          isWantToContinueHere=true;
          break;

        case DialogInterface.BUTTON_NEGATIVE:
          onBackPressed();
          break;
      }
    }
  };

  @Override
  public void onConnectionDestroyed(Session session, Connection connection) {
    connectionMetaData.remove(connection.getData());
  }
}

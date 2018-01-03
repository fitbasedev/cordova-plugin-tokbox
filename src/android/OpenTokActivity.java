package com.fitbase.TokBox;

import android.Manifest;
import android.app.Dialog; 
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fitbase.MainActivity;
import com.fitbase.R;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Session.StreamPropertiesListener;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import org.json.JSONObject;

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


public class OpenTokActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        Publisher.PublisherListener,
        Session.SessionListener, Session.ReconnectionListener ,Subscriber.VideoListener,Session.SignalListener,Session.ConnectionListener{

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final int RC_SETTINGS_SCREEN_PERM = 123;
  private static final int RC_VIDEO_APP_PERM = 124;

  private Session mSession;
  private Publisher mPublisher;
long time;
  private ArrayList<Subscriber> mSubscribers = new ArrayList<Subscriber>();
  private HashMap<Stream, Subscriber> mSubscriberStreams = new HashMap<Stream, Subscriber>();

  Set<String> connectionMetaData = new HashSet<String>(); 
  //  private ConstraintLayout mContainer;
  private RelativeLayout mPublisherViewContainer;
  private RelativeLayout mSubscriberViewContainer;
  private ImageView mLocalAudioOnlyImage,avatar;
  private ProgressDialog mProgressDialog,mSessionReconnectDialog;
  String key;

  private String tokBoxData, apiKey, token, sessionId, publisherId, duration, startdate,logedInUserId;
private RelativeLayout actionBar;
  ImageButton btnPausevideo, btnPauseaudio, btn_exit;
  private Handler hidehandler;
  LinearLayout llcontrols;
  private TextView tvtimer,init_info,  mAlert;
  float dX, dY;
  private static final String FORMAT_2 = "%02d";
  private int totalConnections=0;
    public static final String SIGNAL_TYPE = "closeConnection";
  public boolean isWantToContinueHere;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
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


//    mContainer = (ConstraintLayout) findViewById(R.id.main_container);
    // initialize view objects from your layout
    mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisher_container);
    mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriber_container);
    btnPausevideo = (ImageButton) findViewById(R.id.btn_pausevideo);
    btnPauseaudio = (ImageButton) findViewById(R.id.btn_pauseaudio);
    btn_exit = (ImageButton) findViewById(R.id.btn_exit);
    llcontrols = (LinearLayout) findViewById(R.id.llcontrols);
    tvtimer = (TextView) findViewById(R.id.tvtimer);
    init_info=(TextView)findViewById(R.id.init_info);
    mAlert = (TextView) findViewById(R.id.quality_warning);
    mSessionReconnectDialog = new ProgressDialog(OpenTokActivity.this);
   //for dmaking draggable
    // /* mPublisherViewContainer.setOnTouchListener(this);*/
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
    /*
    *params @totalTime in millis, @Time ticks
     */



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
        this.getResources().getDisplayMetrics().widthPixels, this.getResources()
        .getDisplayMetrics().heightPixels);
      avatar = new ImageView(this);
      avatar.setImageResource(R.mipmap.avatar);
      avatar.setBackgroundResource(R.drawable.bckg_audio_only);
      mSubscriberViewContainer.addView(avatar,layoutParams);
    } else {
      mSubscriberViewContainer.removeView(avatar);
    }
  }
  public void swapCamera(View view) {

      mPublisher.cycleCamera();

  }
  private Runnable hideControllerThread = new Runnable() {

    public void run() {
//      volumeBar.setVisibility(View.GONE);
//      audioControllView.setVisibility(View.GONE);
//      topBar.setVisibility(View.GONE);
      llcontrols.setVisibility(View.GONE);
//      RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
//      params.setMargins(10, 0, 0, 10);
//      mSubscriberViewContainer.setLayoutParams(params);
//      btnPauseaudio.setVisibility(View.GONE);
    }
  };


  public void hideControllers() {
    hidehandler.postDelayed(hideControllerThread, 10000);
  }

  public void showControllers() {
//    volumeBar.setVisibility(View.VISIBLE);
//    topBar.setVisibility(View.VISIBLE);
//    audioControllView.setVisibility(View.VISIBLE);
    llcontrols.setVisibility(View.VISIBLE);
//    RelativeLayout.LayoutParams params=new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
//    params.setMargins(10, 0, 0, 60);
//    mSubscriberViewContainer.setLayoutParams(params);
//    btnPauseaudio.setVisibility(View.VISIBLE);
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

  private void startPublisherPreview() {
    mPublisher = new Publisher.Builder(this).name("publisher").build();
    mPublisher.setPublisherListener(this);
    mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    mPublisher.startPreview();
  }

  @AfterPermissionGranted(RC_VIDEO_APP_PERM)
  private void requestPermissions() {
    String[] perms = {
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    if (EasyPermissions.hasPermissions(this, perms)) {
      mSession = new Session.Builder(this, apiKey, sessionId).sessionOptions(new Session.SessionOptions() {
        @Override
        public boolean useTextureViews() {
          return true;
        }
      }).build();
      mSession.setSessionListener(this);
      mSession.setReconnectionListener(this);
      if(mSession!=null){
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.setConnectionListener(this);
        mSession.connect(token);
        startPublisherPreview();
        mPublisher.getView().setId(R.id.publisher_view_id);
        mPublisherViewContainer.addView(mPublisher.getView());
        //show connecting dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.show();
        //show the messgae when trainer not joined the session
        init_info.setBackgroundResource(R.color.quality_warning);
        init_info.setTextColor(OpenTokActivity.this.getResources().getColor(R.color.white));
        init_info.bringToFront();
        init_info.setVisibility(View.VISIBLE);
      } else {
        Log.e(TAG, "OpenTok credentials are invalid");
        Toast.makeText(OpenTokActivity.this, "Credentials are invalid", Toast.LENGTH_LONG).show();
        this.finish();
      }

//      mContainer.addView(mPublisher.getView());
//      calculateLayout();
    } else {
      EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), RC_VIDEO_APP_PERM, perms);
    }
  }

  @Override
  public void onConnected(Session session) {
    Log.d(TAG, "onConnected: Connected to session " + session.getSessionId());
    key=session.getConnection().getData();
    connectionMetaData.add(key); 
    mSession.publish(mPublisher);
    mProgressDialog.dismiss();
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

  private int getResIdForSubscriberIndex(int index) {
    TypedArray arr = getResources().obtainTypedArray(R.array.subscriber_view_ids);
    int subId = arr.getResourceId(index, 0);
    arr.recycle();
    return subId;
  }

  @Override
  public void onStreamReceived(Session session, Stream stream) {
    Log.d(TAG, "onStreamReceived: New stream " + stream.getStreamId() + " in session " + session.getSessionId());

    String publisherId = stream.getConnection().getData();

    if (this.publisherId.equalsIgnoreCase(publisherId)) {
     //show loader when subscriber is joining a session

      final Subscriber subscriber = new Subscriber.Builder(OpenTokActivity.this, stream).build();
      init_info.setVisibility(View.GONE);
      subscriber.setVideoListener(OpenTokActivity.this);
      mSession.subscribe(subscriber);
      mSubscribers.add(subscriber);
      mSubscriberStreams.put(stream, subscriber);

      int subId = getResIdForSubscriberIndex(mSubscribers.size() - 1);
      subscriber.getView().setId(subId);
//      mContainer.addView(subscriber.getView());


      mSubscriberViewContainer.addView(subscriber.getView());
    //stop loading spinning



//      calculateLayout();
    }
  }

  @Override
  public void onStreamDropped(Session session, Stream stream) {
    Log.d(TAG, "onStreamDropped: Stream " + stream.getStreamId() + " dropped from session " + session.getSessionId());

    Subscriber subscriber = mSubscriberStreams.get(stream);
    if (subscriber == null) {
      return;
    }

    mSubscribers.remove(subscriber);
    mSubscriberStreams.remove(stream);
//    mContainer.removeView(subscriber.getView());
    mSubscriberViewContainer.removeView(subscriber.getView());
    mSubscriberViewContainer.removeView(avatar);
    init_info.setBackgroundResource(R.color.quality_warning);
    init_info.setTextColor(OpenTokActivity.this.getResources().getColor(R.color.white));
    init_info.bringToFront();
    init_info.setVisibility(View.VISIBLE);
    // Recalculate view Ids
    for (int i = 0; i < mSubscribers.size(); i++) {
      mSubscribers.get(i).getView().setId(getResIdForSubscriberIndex(i));
    }
//    calculateLayout();
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
    mProgressDialog.dismiss();
    Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
    finish();
  }

//  private void calculateLayout() {
//    ConstraintSetHelper set = new ConstraintSetHelper(R.id.main_container);
//
//    int size = mSubscribers.size();
//    if (size == 0) {
//      // Publisher full screen
//      set.layoutViewFullScreen(R.id.publisher_view_id);
//    } else if (size == 1) {
//      // Publisher
//      // Subscriber
//      set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(0));
//      set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
//      set.layoutViewWithBottomBound(getResIdForSubscriberIndex(0), R.id.main_container);
//      set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
//      set.layoutViewAllContainerWide(getResIdForSubscriberIndex(0), R.id.main_container);
//
//    } else if (size > 1 && size % 2 == 0) {
//      //  Publisher
//      // Sub1 | Sub2
//      // Sub3 | Sub4
//      //    .....
//      set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
//      set.layoutViewAllContainerWide(R.id.publisher_view_id, R.id.main_container);
//
//      for (int i = 0; i < size; i += 2) {
//        if (i == 0) {
//          set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
//          set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i + 1));
//        } else {
//          set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
//          set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
//        }
//
//        set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
//      }
//
//      set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
//      set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
//    } else if (size > 1) {
//      // Pub  | Sub1
//      // Sub2 | Sub3
//      // Sub3 | Sub4
//      //    .....
//
//      set.layoutViewWithTopBound(R.id.publisher_view_id, R.id.main_container);
//      set.layoutViewWithTopBound(getResIdForSubscriberIndex(0), R.id.main_container);
//      set.layoutTwoViewsOccupyingAllRow(R.id.publisher_view_id, getResIdForSubscriberIndex(0));
//
//      for (int i = 1; i < size; i += 2) {
//        if (i == 1) {
//          set.layoutViewAboveView(R.id.publisher_view_id, getResIdForSubscriberIndex(i));
//          set.layoutViewAboveView(getResIdForSubscriberIndex(0), getResIdForSubscriberIndex(i + 1));
//        } else {
//          set.layoutViewAboveView(getResIdForSubscriberIndex(i - 2), getResIdForSubscriberIndex(i));
//          set.layoutViewAboveView(getResIdForSubscriberIndex(i - 1), getResIdForSubscriberIndex(i + 1));
//        }
//        set.layoutTwoViewsOccupyingAllRow(getResIdForSubscriberIndex(i), getResIdForSubscriberIndex(i + 1));
//      }
//
//      set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 2), R.id.main_container);
//      set.layoutViewWithBottomBound(getResIdForSubscriberIndex(size - 1), R.id.main_container);
//    }
//
//    set.applyToLayout(mContainer, true);
//  }

  private void disconnectSession() {
    if (mSession == null) {
      return;
    }

    if (mSubscribers.size() > 0) {
      for (Subscriber subscriber : mSubscribers) {
        if (subscriber != null) {
          mSession.unsubscribe(subscriber);
          subscriber.destroy();
        }
      }
    }

    if (mPublisher != null) {
      mSession.unpublish(mPublisher);
//      mContainer.removeView(mPublisher.getView());
      mPublisherViewContainer.removeView(mPublisher.getView());
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
      AlertDialog.Builder builder = new AlertDialog.Builder(OpenTokActivity.this); 
      builder.setMessage("Session has been reconnected")
        .setPositiveButton(android.R.string.ok, null);
      builder.create();
      builder.show();
    }
  }
    @Override
  public void onConnectionCreated(Session session, Connection connection) {
    totalConnections++;
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

  }

  @Override
  public void onVideoDataReceived(SubscriberKit subscriberKit) {

  }

  @Override
  public void onVideoDisabled(SubscriberKit subscriberKit, String s) {
    onDisableRemoteVideo(false);
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

  @Override
  public void onSignalReceived(Session session, String type, String data, Connection connection) {

    if (!isWantToContinueHere && type != null && type.equals(SIGNAL_TYPE) && data.equals(logedInUserId)) {
    onBackPressed();
    }
  }
  //move self video on screen draggable

 /* public boolean onTouch(View view, MotionEvent event) {

    switch (event.getAction()) {

      case MotionEvent.ACTION_DOWN:

        dX = view.getX() - event.getRawX();
        dY = view.getY() - event.getRawY();
        break;

      case MotionEvent.ACTION_MOVE:

        view.animate()
          .x(event.getRawX() + dX)
          .y(event.getRawY() + dY)
          .setDuration(0)
          .start();
        break;
      default:
        return false;
    }
    return true;
  }*/

}

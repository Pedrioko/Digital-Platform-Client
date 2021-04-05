package com.example.digitalplatform;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.session.MediaController;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;

import static org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    public static final String URL = "http://192.168.1.100:3000/";
    public static final String CHANNEL_ID = "ida12";
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private GeckoView geckoview;
    private String url;
    private GeckoSession geckoSession;
    private GeckoRuntime geckoruntime;
    private NotificationManager notificationManager;
    private boolean fullscreen;
    public static WebExtension.Port WEPORT = null;
    private static final int REQUEST_FILE_PICKER = 1;
    private static int BACK_CODE = 1;
    private static int NEXT_CODE = 3;

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;

    private int mCurrentState;

    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;


    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mMediaControllerCompat = new MediaControllerCompat(FullscreenActivity.this, mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                setMediaController((MediaController) mMediaControllerCompat.getMediaController());

            } catch (Exception e) {

            }
        }
    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if (state == null) {
                return;
            }

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    mCurrentState = STATE_PLAYING;
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    mCurrentState = STATE_PAUSED;
                    break;
                }
            }
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private WebExtension.MessageDelegate createMessageDelegate() {
        return new WebExtension.MessageDelegate() {

            @Nullable
            @Override
            public GeckoResult<Object> onMessage(@NonNull String nativeApp, @NonNull Object message, @NonNull WebExtension.MessageSender sender) {
                Log.d("MessageDelegate", message.toString());
                return null;
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        createNotificationChannel();
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        filter.setPriority(1000);
        registerReceiver(r, filter);
        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        geckoview = findViewById(R.id.geckoview);
        GeckoRuntimeSettings runtimeSettings = new GeckoRuntimeSettings.Builder()
                .remoteDebuggingEnabled(true)
                .build();
        geckoruntime = GeckoRuntime.create(this, runtimeSettings);
        GeckoSessionSettings.Builder settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(true)
                .userAgentMode(USER_AGENT_MODE_MOBILE)
                .userAgentOverride("")
                .suspendMediaWhenInactive(true)
                .allowJavascript(true)
                .useTrackingProtection(true)
                .displayMode(GeckoSessionSettings.DISPLAY_MODE_FULLSCREEN);
        geckoSession = new GeckoSession(settings.build());
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(@NonNull GeckoSession session, @Nullable String newurl) {
                url = newurl;
                if (newurl.toLowerCase().contains("playlist")) {

                    Notification noti = buildNotification();

                    notificationManager.notify(1, noti);
                }
            }
        });
        // This delegate will handle all communications from and to a specific Port
        // object
        WebExtension.PortDelegate portDelegate = new WebExtension.PortDelegate() {

            public void onPortMessage(final @NonNull Object message,
                                      final @NonNull WebExtension.Port port) {
                // This method will be called every time a message is sent from the
                // extension through this port. For now, let's just log a
                // message.
                Log.d("PortDelegate", "Received message from WebExtension: "
                        + message);
            }

            public void onDisconnect(final @NonNull WebExtension.Port port) {
                WEPORT = null;
            }
        };
        // This delegate will handle requests to open a port coming from the
        // extension
        WebExtension.MessageDelegate messageDelegate = new WebExtension.MessageDelegate() {
            @Nullable
            @Override
            public GeckoResult<Object> onMessage(final @NonNull String nativeApp,
                                                 final @NonNull Object message,
                                                 final @NonNull WebExtension.MessageSender sender) {
                if (message instanceof JSONObject) {
                    JSONObject json = (JSONObject) message;
                    try {
                        if (json.has("type") && "WPAManifest".equals(json.getString("type"))) {
                            JSONObject manifest = json.getJSONObject("manifest");
                            Log.d("MessageDelegate", "Found WPA manifest: " + manifest);
                        }
                    } catch (JSONException ex) {
                        Log.e("MessageDelegate", "Invalid manifest", ex);
                    }
                }
                return null;
            }

            @Override
            public void onConnect(final @NonNull WebExtension.Port port) {
                // Let's store the Port object in a member variable so it can be
                // used later to exchange messages with the WebExtension.
                WEPORT = port;

                // Registering the delegate will allow us to receive messages sent
                // through this port.
                WEPORT.setDelegate(portDelegate);
            }
        };

        geckoruntime.getWebExtensionController()
                .ensureBuiltIn("resource://android/assets/messaging/", "messaging@example.com")
                .accept((extension) -> {
                            if (extension != null) {
                                extension.setMessageDelegate(messageDelegate, "browser");
                            }
                            geckoSession.getWebExtensionController().setMessageDelegate(extension, messageDelegate, "browser");
                            Log.i("MessageDelegate", "Extension installed: " + extension);
                        },
                        e -> {
                            Log.e("MessageDelegate", "Error registering WebExtension", e);
                        }
                );

        geckoSession.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public void onMediaPermissionRequest(@NonNull GeckoSession session, @NonNull String uri, @Nullable MediaSource[] video, @Nullable MediaSource[] audio, @NonNull MediaCallback callback) {
                System.out.println("GG");
            }
        });


        geckoSession.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession session, boolean fullScreen) {
                if (fullScreen) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    fullscreen = true;
                } else {
                    fullscreen = false;

                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                    session.exitFullScreen();
                }
            }
        });

        geckoSession.setMediaSessionDelegate(new org.mozilla.geckoview.MediaSession.Delegate() {
            @Override
            public void onActivated(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {

            }

            @Override
            public void onDeactivated(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {

            }

            @Override
            public void onMetadata(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession, @NonNull org.mozilla.geckoview.MediaSession.Metadata meta) {

            }

            @Override
            public void onFeatures(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession, long features) {

            }

            @Override
            public void onPlay(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
                System.out.println("VENJVEO");

            }

            @Override
            public void onPause(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
                System.out.println("VENJVEO");

            }

            @Override
            public void onStop(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession) {
                System.out.println("VENJVEO");

            }

            @Override
            public void onPositionState(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession, @NonNull org.mozilla.geckoview.MediaSession.PositionState state) {
                System.out.println("VENJVEO");

            }

            @Override
            public void onFullscreen(@NonNull GeckoSession session, @NonNull org.mozilla.geckoview.MediaSession mediaSession, boolean enabled, @Nullable org.mozilla.geckoview.MediaSession.ElementMetadata meta) {
                fullscreen = true;
                System.out.println("VENJVEO");
            }
        });


        mContentView.setOnClickListener(view -> toggle());

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        final BasicGeckoViewPrompt prompt = new BasicGeckoViewPrompt(this);
        prompt.filePickerRequestCode = REQUEST_FILE_PICKER;

        geckoSession.setPromptDelegate(prompt);
        geckoSession.open(geckoruntime);
        geckoview.setSession(geckoSession);
        geckoSession.loadUri(URL); // Or any other URL...
    }

    private Notification buildNotification() {
        //This is the intent of PendingIntent
        Intent intentAction = new Intent(getBaseContext(), ActionReceiver.class);
        intentAction.putExtra("action", "back");
        PendingIntent prev = PendingIntent.getBroadcast(this, BACK_CODE, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);

        intentAction = new Intent(getBaseContext(), ActionReceiver.class);
        intentAction.putExtra("action", "next");
        PendingIntent next = PendingIntent.getBroadcast(this, NEXT_CODE, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_nt)
                .setContentTitle("Track title")
                .setContentText("Artist - Album")
                .addAction(R.drawable.ic_prev, "Previous", prev) // #0
                .addAction(R.drawable.ic_pause, "Pause", null)  // #1
                .addAction(R.drawable.ic_next, "Next", next)
                .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        }
    }

    @Override
    public void onBackPressed() {
        try {
            JSONObject message = new JSONObject();
            message.put("keyCode", "S");
            WEPORT.postMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (url.equalsIgnoreCase(URL)) {
            super.onBackPressed();
        } else {
            if (fullscreen) {
                geckoSession.exitFullScreen();
            } else
                geckoSession.goBack();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            geckoruntime.orientationChanged();
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            geckoruntime.orientationChanged();
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hide();
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
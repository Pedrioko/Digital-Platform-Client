package com.example.digitalplatform;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.util.List;

import static org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    public static final String URL = "http://192.168.1.100:3000/";
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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    private GeckoView geckoview;
    private String url;
    private GeckoSession geckoSession;
    private GeckoRuntime geckoruntime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);


        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        geckoview = findViewById(R.id.geckoview);
        geckoruntime = GeckoRuntime.create(this);
        GeckoSessionSettings.Builder settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(true)
                .useTrackingProtection(true)
                .userAgentMode(USER_AGENT_MODE_MOBILE)
                .userAgentOverride("")
                .suspendMediaWhenInactive(true)
                .allowJavascript(true)
                .displayMode(GeckoSessionSettings.DISPLAY_MODE_FULLSCREEN);
        geckoSession = new GeckoSession(settings.build());
        geckoSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(@NonNull GeckoSession session, @Nullable String newurl) {
                url = newurl;
            }
        });
        geckoSession.open(geckoruntime);
        geckoview.setSession(geckoSession);
        geckoSession.loadUri(URL); // Or any other URL...


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
                } else {
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
                System.out.println("VENJVEO");
            }
        });
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
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
        if (url.equalsIgnoreCase(URL)) {
            super.onBackPressed();
        } else {
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
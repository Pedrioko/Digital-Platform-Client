package com.example.digitalplatform;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this,
                MediaPlaybackService.class.getSimpleName());

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
              MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
              MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                            .setActions(
                                PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                System.out.println("Test");
            }

            @Override
            public void onPause() {
                super.onPause();
                System.out.println("Test");

            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                System.out.println("Test");
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                System.out.println("Test");
            }

            @Override
            public void onStop() {
                super.onStop();
                System.out.println("Test");
            }
        });

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }


    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid, Bundle rootHints) {
        // Returning null == no one can connect
        // so we’ll return something
        return new BrowserRoot(
                getString(R.string.app_name), // Name visible in Android Auto
                null); // Bundle of optional extras
    }
    @Override
    public void onLoadChildren(String parentId,
                               Result<List<MediaBrowserCompat.MediaItem>> result) {
        // I promise we’ll get to browsing
        result.sendResult(null);
    }

}
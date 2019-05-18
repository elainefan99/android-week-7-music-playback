package com.ucsdextandroid1.musicsearch2.player;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ucsdextandroid1.musicsearch2.data.SongItem;

/**
 * Created by rjaylward on 2019-05-16
 */
public class MusicPlayerService extends Service {

    public static final String ACTION_STATE_CHANGED = "musicsearch.state_changed";

    public static final String EXTRA_PLAYBACK_STATE = "musicsearch.playback_state";
    public static final String EXTRA_SONG = "musicsearch.song";

    private static final String ACTION_PLAY = "musicsearch.play";
    private static final String ACTION_RESUME = "musicsearch.resume";
    private static final String ACTION_PAUSE = "musicsearch.pause";
    private static final String ACTION_STOP = "musicsearch.stop";

    @Nullable private MusicPlayer musicPlayer;
private int musicPlayerState = MusicPlayer.STATE_STOPPED;
    private static Intent createIntent(Context context, String action) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(action);
        return intent;
    }

    public static void play(Context context, SongItem songItem) {
        Intent intent = new Intent(context, MusicPlayerService.class);
                intent .setAction(ACTION_PLAY);
                intent.putExtra(EXTRA_SONG, songItem);
        context.startService(intent);

    }

    public static void resume(Context context) {
        Intent intent = new Intent(context, MusicPlayer.class);
        intent.setAction(ACTION_RESUME);
        context.startService(intent);
    }
    public static void pause(Context context) {
        context.startService(createIntent(context, ACTION_PAUSE));
    }

    public static void stop(Context context) {
        context.startService(createIntent(context, ACTION_STOP));
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if(intent != null) {
           if(ACTION_PLAY.equals(intent.getAction())) {
               SongItem songItem = intent.getParcelableExtra(EXTRA_SONG);
                if(songItem != null) {
                    play(songItem);
                }
            }
            else if(musicPlayer != null && intent.getAction() != null) {
                switch(intent.getAction()) {
                    case ACTION_RESUME:
                        resume();
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                        case ACTION_STOP:
                        stop();
                        break;
                }
            }
            else
                stopSelf();
        }
        else
            stopSelf();

        return super.onStartCommand(intent, flags, startId);
    }

    @NonNull
    private MusicPlayer createIfNecessary() {
        if(musicPlayer == null) {
            musicPlayer = new MusicPlayer();

            musicPlayer.setStateChangedListener(new MusicPlayer.OnStateChangedListener() {

                @Override
                public void onStateChanged(int state) {
                    musicPlayerState = state;
                    Intent intent = new Intent(ACTION_STATE_CHANGED);
                    intent.putExtra(EXTRA_SONG, musicPlayer.getCurrentSongItem());
                    intent.putExtra(EXTRA_PLAYBACK_STATE, state);

                    LocalBroadcastManager.getInstance(MusicPlayerService.this).sendBroadcast(intent);

                    MusicController.getInstance().updatePlaybackState(state);
                }

            });
        }

        return musicPlayer;
    }

    private void play(@NonNull SongItem songItem) {
        createIfNecessary().play(songItem);

        MusicController.getInstance().updateMetadata(songItem);
    }

    private void resume() {
        if(musicPlayer != null)
            musicPlayer.resume();
    }

    private void pause() {
        if(musicPlayer != null)
            musicPlayer.pause();
    }

    private void stop() {
        if(musicPlayer != null)
            musicPlayer.stop();

        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stop();
        musicPlayer = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //not used for now
        return new MusicPlayerServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //not used for now
        return super.onUnbind(intent);
    }
    public int getCurrentState() {
        return musicPlayerState;
    }
    public SongItem getCurrentSong(){
        return musicPlayer !=null? musicPlayer.getCurrentSongItem(): null;
    }
   public class MusicPlayerServiceBinder extends Binder{




         public MusicPlayerService getService(){
             return MusicPlayerService.this;
        }
    }
}


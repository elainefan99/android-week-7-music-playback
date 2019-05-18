package com.ucsdextandroid1.musicsearch2.search;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.ucsdextandroid1.musicsearch2.player.MusicDetailsActivity;
import com.ucsdextandroid1.musicsearch2.R;
import com.ucsdextandroid1.musicsearch2.data.DataSources;
import com.ucsdextandroid1.musicsearch2.data.SongItem;
import com.ucsdextandroid1.musicsearch2.player.MusicControlsManager;
import com.ucsdextandroid1.musicsearch2.player.MusicPlayer;
import com.ucsdextandroid1.musicsearch2.player.MusicPlayerService;
import com.ucsdextandroid1.musicsearch2.utils.Debouncer;

import java.util.Collections;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {

    private SearchAdapter searchAdapter;
    private String latestSearchTerm;
    private MusicControlsManager musicControls;
    private MusicPlayer musicPlayer;
    private int currentState;
    private SongItem currentSong;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchAdapter = new SearchAdapter();

        RecyclerView recyclerView = findViewById(R.id.am_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(searchAdapter);

        EditText editText = findViewById(R.id.am_toolbar_edit_text);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                debouncer.onChange(s.toString());
            }
        });

        View arrowView = findViewById(R.id.am_up_arrow);
        arrowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(MusicDetailsActivity.createIntent(SearchActivity.this, currentState, currentSong));
            }
        });

        setUpMusicControls();
    }

    private void setUpMusicControls() {
       // musicPlayer = new MusicPlayer();
      //  musicPlayer.setStateChangedListener(new MusicPlayer.OnStateChangedListener() {
      //  @Override
      //      public void onStateChanged(int state) {
      //         musicControls.updateViewState(state);
      //      }
       //     int state = intent.intent
      //  });
        musicControls = MusicControlsManager.newUIBuilder()
                .setVisibilityRootView(findViewById(R.id.am_controls_group))
                .setLabelView(findViewById(R.id.am_controls_label))
                .setLoadingView(findViewById(R.id.am_controls_loading))
                .setPlayPauseView(
                        findViewById(R.id.am_controls_play_pause),
                        R.drawable.ic_play_circle_outline,
                        R.drawable.ic_pause_circle_outline
                ).build();

        musicControls.setControlsClickListener(new MusicControlsManager.OnControlClickListener() {
            @Override
            public void onResumeClicked() {
//                musicPlayer.resume();
                MusicPlayerService.resume(SearchActivity.this);
            }

            @Override
            public void onPauseClicked() {
//                musicPlayer.pause();
                MusicPlayerService.pause(SearchActivity.this);
            }
        });

        searchAdapter.setOnItemClickListener(new OnItemClickListener<SongItem>() {
            @Override
            public void onItemClicked(SongItem item) {
          //  musicPlayer.play(item);
          //  musicControls.updateViewMetadata(item);
                MusicPlayerService.play(SearchActivity.this, item);
              //  musicControls.updateViewState(MusicPlayer.STATE_PLAYING);
            }
        });

       registerBroadcastReceiver();
    }

    /**
     * Debouncer keeps us from making a bunch of api calls if the text changes too quickly. We will
     * make at most one search every 200 milliseconds even if the text changes faster than that.
     */
    private Debouncer<String> debouncer = Debouncer.create(200, new Debouncer.ChangeListener<String>() {
        @Override
        public void onChange(String item) {
            performSearch(item);
        }
    });

    private void performSearch(String term) {
        String trimmedTerm = term.trim();

        if(!Objects.equals(trimmedTerm, latestSearchTerm)) {
            latestSearchTerm = trimmedTerm;
            if(TextUtils.isEmpty(trimmedTerm)) {
                searchAdapter.submitList(Collections.emptyList());
            }
            else {
                DataSources.getInstance().search(trimmedTerm, data -> {
                    if(!TextUtils.isEmpty(latestSearchTerm))
                        searchAdapter.submitList(data);
                });
            }
        }
    }


    private void registerBroadcastReceiver() {

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(controlsBroadcastReceiver,
                new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED));
    }

    private void unregisterBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(controlsBroadcastReceiver);
    }

    private BroadcastReceiver controlsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(MusicPlayerService.EXTRA_PLAYBACK_STATE, MusicPlayer.STATE_STOPPED);
                SongItem item = intent.getParcelableExtra(MusicPlayerService.EXTRA_SONG);

                if(musicControls != null) {
                    musicControls.updateViewState(state);
                    musicControls.updateViewMetadata(item);

                }
                currentState = state;
                currentSong = item;
            }
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterBroadcastReceiver();
    }

}

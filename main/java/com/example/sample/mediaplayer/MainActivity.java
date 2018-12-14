package com.example.sample.mediaplayer;

import android.Manifest;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.database.Cursor;
import android.util.Log;
import  android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.app.Dialog;

public class MainActivity extends AppCompatActivity {


    private int PERMISSIONS_REQUEST_ACCESS_PHONE_STATE = 1;
    private int PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_SD = 2;
    private ImageButton forwardbtn, backwardbtn, pausebtn, playbtn, shufflebtn, loopbtn;
    private MediaPlayer mPlayer;
    private TextView songName, startTime, songTime;
    private SeekBar songPrgs;
    private ListView songsList;
    private static int oTime =0, sTime =0, eTime =0, fTime = 5000, bTime = 5000;
    private Handler hdlr = new Handler();
    private ArrayList<AudioFile> songs;
    private CustomAdapter customAdapter;
    private  AudioFile lastSong;
    private MediaPlayerService player;
    boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.sample.mediaplayer.PlayNewAudio";
    public static final String Broadcast_PAUSE = "com.example.sample.mediaplayer.PauseAudio";
    public static final String Broadcast_NEXT = "com.example.sample.mediaplayer.PlayNewAudio";
    public static final String Broadcast_PLAY_PREVIOUS = "com.example.sample.mediaplayer.PlayNewAudio";

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("INCOMMING_CALL_DECLINE")) {
                Log.i("text", "text");
                Play(lastSong);
            } else if(intent.getAction().equals("INCOMMING_CALL")) {
                Log.i("text", "1text");
                Pause();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        backwardbtn = (ImageButton)findViewById(R.id.btnBackward);
        forwardbtn = (ImageButton)findViewById(R.id.btnForward);
        playbtn = (ImageButton)findViewById(R.id.btnPlay);
        shufflebtn = (ImageButton)findViewById(R.id.btnShuffle);
        loopbtn = (ImageButton)findViewById(R.id.btnLoop);
        songsList = (ListView) findViewById(R.id.songsList);
        pausebtn = (ImageButton)findViewById(R.id.btnPause);
        songName = (TextView)findViewById(R.id.txtSname);
        startTime = (TextView)findViewById(R.id.txtStartTime);
        songTime = (TextView)findViewById(R.id.txtSongTime);
        songPrgs = (SeekBar)findViewById(R.id.sBar);
        songPrgs.setClickable(false);
        pausebtn.setEnabled(false);


        songs = new ArrayList();
        mPlayer = new MediaPlayer();
        registerReceiver(broadcastReceiver, new IntentFilter("INCOMMING_CALL_DECLINE"));
        registerReceiver(broadcastReceiver, new IntentFilter("INCOMMING_CALL"));

        customAdapter = new CustomAdapter(this, R.layout.list_item, songs);
        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                AudioFile selectedSong = (AudioFile)parent.getItemAtPosition(position);
                //Play(songs.get(position));
                playAudio(position);
            }
        };
        songsList.setOnItemClickListener(itemListener);

        playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Play(lastSong);
            }
        });

        pausebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pause();
                //Toast.makeText(getApplicationContext(),"Pausing Audio", Toast.LENGTH_SHORT).show();
            }
        });

        forwardbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((sTime + fTime) <= eTime)
                {
                    sTime = sTime + fTime;
                    mPlayer.seekTo(sTime);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
                }
                if(!playbtn.isEnabled()){
                    playbtn.setEnabled(true);
                }
            }
        });
        backwardbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((sTime - bTime) > 0)
                {
                    sTime = sTime - bTime;
                    mPlayer.seekTo(sTime);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show();
                }
                if(!playbtn.isEnabled()){
                    playbtn.setEnabled(true);
                }
            }
        });

        loopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               MediaPlayerService.SetLoop();
            }
        });

        CheckPermission(Manifest.permission.READ_PHONE_STATE, PERMISSIONS_REQUEST_ACCESS_PHONE_STATE);
        CheckPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_SD);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songs);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            hdlr.postDelayed(UpdateSongTime, 100);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


    private void pauseAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(songs);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            songName.setText(MediaPlayerService.activeAudio.getTitle());
            Log.i("qext", MediaPlayerService.activeAudio.getArtist());
            sTime = MediaPlayerService.GetPosition();
            eTime = MediaPlayerService.GetDuration();
            songTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(eTime),
                    TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(eTime))) );
            startTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(sTime),
                    TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(sTime))) );
            songPrgs.setMax(eTime);
            songPrgs.setProgress(sTime);

            hdlr.postDelayed(this, 100);
        }
    };

    protected  void LoadSongs(){
        ContentResolver cr = this.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur;
        cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;
        String data = "";
        if(cur != null)
        {
            count = cur.getCount();
            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String duration = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    songs.add(new AudioFile(data, title, title, title));
                }

            }
        }
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        songsList.setAdapter(customAdapter);
        cur.close();
    }

    public  void CheckPermission(String permission, int permission_access){
        if (ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED) {
            RequestPermission(permission, permission_access);
        } else  if(permission_access == PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_SD) {
            LoadSongs();
        }
    }

    public  void RequestPermission(String permission, int permission_access){
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission},permission_access);
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_PHONE_STATE) {
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.i("Permission", "ok");
            } else {
                Log.i("Permission", "not ok");
            }
        } else if(requestCode == PERMISSIONS_REQUEST_ACCESS_READ_EXTERNAL_SD){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                LoadSongs();
                Log.i("Permission", "Songs loaded");
            } else {
                Log.i("Permission", "not ok");
            }
        }
    }

    private  void Pause(){
        mPlayer.pause();
        pausebtn.setEnabled(false);
        playbtn.setEnabled(true);
    }

    private  void Play(AudioFile song){
//        if(lastSong==null)
//            return;
//        try {
//            mPlayer.reset();
//            mPlayer.setDataSource(song.getData());
//        } catch (IOException err){
//            Log.e("Error", err.getMessage());
//        }
//        try {
//            mPlayer.prepare();
//        } catch (IllegalStateException e) {
//            Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
//        } catch (IOException e) {
//            Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
//        }
//        mPlayer.start();
//        eTime = mPlayer.getDuration();
//        sTime = mPlayer.getCurrentPosition();
//        if(oTime == 0){
//            songPrgs.setMax(eTime);
//            oTime =1;
//        }
//        songName.setText(song.getTitle());
//        songTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(eTime),
//                TimeUnit.MILLISECONDS.toSeconds(eTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(eTime))) );
//        startTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(sTime),
//                TimeUnit.MILLISECONDS.toSeconds(sTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS. toMinutes(sTime))) );
//        songPrgs.setProgress(sTime);
//        hdlr.postDelayed(UpdateSongTime, 100);
//        pausebtn.setEnabled(true);
//        playbtn.setEnabled(false);
    }


    public String convertDuration(String ml) {
        long duration = Long.parseLong(ml);
        String out = null;
        long hours=0;
        try {
            hours = (duration / 3600000);
        } catch (Exception e) {
            e.printStackTrace();
            return out;
        }
        long remaining_minutes = (duration - (hours * 3600000)) / 60000;
        String minutes = String.valueOf(remaining_minutes);
        if (minutes.equals(0)) {
            minutes = "00";
        }
        long remaining_seconds = (duration - (hours * 3600000) - (remaining_minutes * 60000));
        String seconds = String.valueOf(remaining_seconds);
        if (seconds.length() < 2) {
            seconds = "00";
        } else {
            seconds = seconds.substring(0, 2);
        }

        if (hours > 0) {
            out = hours + ":" + minutes + ":" + seconds;
        } else {
            out = minutes + ":" + seconds;
        }

        return out;

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }
}

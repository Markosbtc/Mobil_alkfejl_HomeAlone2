package com.example.homealone2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    public static final int ACTIVITY_RECORD_SOUND = 0;
    private Uri audioFileUri = null;
    private MediaPlayer player = null;
    private ListView mListview;
    private List<String> myList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Button savebtn;


    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = (grantResults.length == 3 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED);
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    public void getPermissionToRecordAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_RECORD_AUDIO_PERMISSION);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.savebtn = findViewById(R.id.button3);
        this.mListview  = findViewById(R.id.theList);
        getPermissionToRecordAudio();
        savebtn.setClickable(false);
        AdapterSettings();
    }

    public void myRecordings(){
        File root = android.os.Environment.getExternalStorageDirectory();
        String path = root.getAbsolutePath() + "/Recordings";
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        //Log.d("Files", "Size: "+ files.length);
        if( files!=null ){

            for (int i = 0; i < files.length; i++) {

                Log.d("Files", "FileName:" + files[i].getAbsolutePath());
                String fileName = files[i].getAbsolutePath();

                myList.add(fileName);
            }
        }
    }

    public void AdapterSettings(){
        myList.clear();
        myRecordings();
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,myList);


        mListview.setAdapter(adapter);
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = mListview.getItemAtPosition(position).toString();
                //Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
                playSelected(str);

            }
        });
    }

    void savefile(Uri sourceuri){
        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/Recordings");
        if (!file.exists()) {
            file.mkdirs();
        }

        //String sourceFilename= sourceuri.getPath();
        //String sourceFilename= String.valueOf(sourceuri);
        String sourceFilename= getRealPathFromURI(sourceuri);
        String destinationFilename =  root.getAbsolutePath() + "/Recordings/" + String.valueOf(System.currentTimeMillis() + ".mp3");

        Log.d("save", "savefile: "+sourceFilename);
        Log.d("save", "savefile: dest: "+destinationFilename);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourceFilename));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Audio.Media.DATA };
            cursor = this.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void playSelected(String path){
        float speed = 0.5f;
        if (player!=null) {
            if(player.isPlaying())
                player.stop();
                player.release();
                player = null;
                Toast.makeText(getApplicationContext(),"Stopped.",Toast.LENGTH_SHORT).show();
                return;
        }
        player = new MediaPlayer();

        try {
            player.setDataSource(path);
            player.prepare();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (player.isPlaying()) {
                    player.stop();
                } else {
                    player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
                    player.start();
                }
            }
        } catch (IOException e) {
            Log.e("AudioPlayer", "prepare() failed");
        }
    }

    public void startRecording(View view) {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, ACTIVITY_RECORD_SOUND);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_RECORD_SOUND) {
            if (resultCode == RESULT_OK) {
                this.audioFileUri = data.getData();
                Log.d("EZ", "onActivityResult: " + data.getData());
                //content://media/external/file/102755
                Log.d("EZ", "getpath: " + audioFileUri.getPath());
                //getpath: /external/file/102772
                Log.d("EZ", "getcontenturiforpath: " +
                        MediaStore.Audio.Media.getContentUriForPath(audioFileUri.getPath()));
                //getcontenturiforpath: content://media/internal/audio/media

                savebtn.setClickable(true);
            }
        }
    }

    public void saveRecording(View view){
        savefile(this.audioFileUri);
        AdapterSettings();
        savebtn.setClickable(false);
    }

    public void playRec(View view){
        float speed = 0.5f;
        if (this.audioFileUri != null) {
            player = new MediaPlayer();
            try {
                player.setDataSource(this,audioFileUri);
                player.prepare();
                // player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
                //Toast.makeText(this,"set playback params", Toast.LENGTH_SHORT).show();
                //player.start();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
                        player.start();
                    }
                }
            } catch (IOException e) {
                Log.e("AudioPlayer", "prepare() failed");
            }

        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }


}

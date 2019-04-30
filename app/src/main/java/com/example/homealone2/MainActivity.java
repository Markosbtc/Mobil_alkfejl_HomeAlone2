package com.example.homealone2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
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
    private static final String FILE_TYPE_NO_MEDIA = ".nomedia";


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
        this.mListview  = findViewById(R.id.theList);
        getPermissionToRecordAudio();


        AdapterSettings();


    }

    public void getRecordings(){
        ContentResolver contentResolver = getContentResolver();
        Uri recordingUri  = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String path = "/storage/emulated/0/MIUI/sound_recorder";

        Cursor cursor = contentResolver.query(recordingUri,null,null,null,null);

        if(cursor!=null && cursor.moveToFirst()){
            int fileColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int title = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);


            do{
                String audioFilePath = cursor.getString(fileColumn);
                String currentTitle = cursor.getString(title);
                String currentDuration = cursor.getString(duration);

                myList.add(audioFilePath);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    public void AdapterSettings(){
        myList.clear();
        getRecordings();
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,myList);

        //adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,filterFiles());

        mListview.setAdapter(adapter);
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = mListview.getItemAtPosition(position).toString();
                //Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
                playSelected(str);
            }
        });


    }





    private ArrayList<String> filterFiles() {

        ArrayList<String> listOfHiddenFiles = new ArrayList<>();
        String hiddenFilePath;

        // Scan all no Media files
        String nonMediaCondition = MediaStore.Files.FileColumns.MEDIA_TYPE
                + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;

        // Files with name contain .nomedia
        String where = nonMediaCondition + " AND "
                + MediaStore.Files.FileColumns.TITLE + " LIKE ?";

        String[] params = new String[] { "%" + FILE_TYPE_NO_MEDIA + "%" };

        // make query for non media files with file title contain ".nomedia" as
        // text on External Media URI
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                new String[] { MediaStore.Files.FileColumns.DATA }, where,
                params, null);

        // No Hidden file found
        if (cursor.getCount() == 0) {

            listOfHiddenFiles.add("No Hidden File Found");

            // show Nothing Found
            return listOfHiddenFiles;
        }

        // Add Hidden file name, path and directory in file object
        while (cursor.moveToNext()) {
            hiddenFilePath = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Files.FileColumns.DATA));
            if (hiddenFilePath != null) {

                listOfHiddenFiles.add(hiddenFilePath.substring(0, hiddenFilePath.lastIndexOf(File.separator)));
            }
        }

        cursor.close();

        return listOfHiddenFiles;

    }

    public boolean isDirHaveImages(String hiddenDirectoryPath) {

        File listFile[] = new File(hiddenDirectoryPath).listFiles();

        boolean dirHaveRecordings = false;

        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {

                if (listFile[i].getName().endsWith(".mp3")
                        /*|| listFile[i].getName().endsWith(".jpg")
                        || listFile[i].getName().endsWith(".jpeg")
                        || listFile[i].getName().endsWith(".gif")*/) {

                    // Break even if folder have a single image file
                    dirHaveRecordings = true;
                    break;

                }
            }
        }
        return dirHaveRecordings;

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


  /*  private void fetchRecordings() {
        myList.clear();
        File root = android.os.Environment.getExternalStorageDirectory();

        String path = root.getAbsolutePath() ;
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        if( files!=null ){

            for (int i = 0; i < files.length; i++) {

                Log.d("Files", "FileName:" + files[i].getName());
                String fileName = files[i].getName();
//                String recordingUri = root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" + fileName;
                String recordingUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI + fileName;
                myList.add(recordingUri);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,myList);
            mListview.setAdapter(adapter);


        }
    }*/

  /*  private void ListDir(@org.jetbrains.annotations.NotNull File f) {
        File[] files = f.listFiles();
        myList.clear();
        Log.d("EZ", "ListDir: Files.length: " + files.length);
        for (File file: files){
            myList.add(file.getPath());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,myList);
        mListview.setAdapter(adapter);
    }*/



 /*   public ArrayList<File> GetFiles(File DirectoryPath) {
        ArrayList<File> myfiles = new ArrayList<File>();
        File[] files = DirectoryPath.listFiles();
        for(File singlefile : files){
            if(singlefile.isDirectory() && !singlefile.isHidden()){
                myfiles.addAll(GetFiles(singlefile));

            }
            else {
                if (singlefile.getName().endsWith("records.mp3")) {
                    myfiles.add(singlefile);
                }
            }
        }

        return myfiles;
    }*/
 /*
    public void listings(){
        final String[] items;
        // String path=Environment.getExternalStorageDirectory();
        final ListView lv=(ListView)findViewById(R.id.theList);
        final ArrayList<File> myrecords = GetFiles(Environment.getExternalStorageDirectory());
        items=new String[myrecords.size()];
        for(int i=0;i<myrecords.size();i++){
            items[i]=myrecords.get(i).getName();
        }
        final ArrayAdapter<String> adapt=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        lv.setAdapter(adapt);
//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                // String filename =(String)lv.getItemAtPosition(position);
//
//                String path=Environment.getExternalStorageDirectory().getAbsolutePath()
//                        +  "/"+(String)lv.getItemAtPosition(position);
//                Toast.makeText(getApplicationContext(),path,Toast.LENGTH_SHORT).show();
//                Uri audio = Uri.parse(path);
//                Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                File file = new File(String.valueOf(audio));
//                intent.setDataAndType(Uri.fromFile(file), "audio/*");
//                startActivity(intent);
//            }
//        });
    }

*/



//    // Checks if external storage is available for read and write
//    public boolean isExternalStorageWritable() {
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            return true;
//        }
//        return false;
//    }


    public void startRecording(View view) {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, ACTIVITY_RECORD_SOUND);
        //  /storage/emulated/0/MIUI/sound_recorder/

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
            }
        }
    }
/*
    public void saveRec(View view){
        if (this.audioFileUri != null){
            //savefile(this.audioFileUri);
            File as = new File(this.audioFileUri.getPath());
            File newfile = new File(this.storageDir,as.toString());
            newfile.mkdirs();
            settingAdapter();
        }
    }
    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e("FILE", "Directory not created");
        }
        return file;
    }
*/
/*
    private void savefile(Uri sourceuri) {
        String sourceFilename= sourceuri.getPath();
        String destinationFilename = android.os.Environment.getExternalStorageDirectory().getPath()+File.separatorChar+"abc.mp3";

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

        /*InputStream in =  getContentResolver().openInputStream("your_uri_here");
        OutputStream out = new FileOutputStream(new File("your_file_here"));
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0){
            out.write(buf,0,len);
        }
        out.close();
        in.close();
    }
*/

    public void playRec(View view){
        float speed = 0.5f;
        if (this.audioFileUri != null) {
            player = new MediaPlayer();
            try {
                player.setDataSource(this,audioFileUri);
                // TODO  player.setDataSource(listItem.getFilePath());
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

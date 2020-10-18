package com.yhr.mediacodecdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MediaMuxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_mux);

        String dir = Environment.getExternalStorageDirectory() + File.separator + "111" + File.separator;
        String audioPath = dir + "V.mp4";   // mp3不行 MediaMuxer#addTrack()会出错
        String videoPath = dir + "A.mp4";
        File outputFile = new File(dir + System.currentTimeMillis() + ".mp4");
        int outputFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

        if (outputFile.exists()) {
            outputFile.delete();
        }
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AVMuxer muxer = new AVMuxer(audioPath, videoPath, outputFile.getAbsolutePath(), outputFormat);
        muxer.setCompleteCallback(this::changeUIWhenMuxComplete);
        muxer.start();
    }

    private void changeUIWhenMuxComplete() {
        TextView tvMux = findViewById(R.id.tv_mux);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> tvMux.setText("complete"));
    }

    public void goToPlay(View v) {
        Intent intent = new Intent(MediaMuxActivity.this, MainActivity.class);
        startActivity(intent);
    }

}
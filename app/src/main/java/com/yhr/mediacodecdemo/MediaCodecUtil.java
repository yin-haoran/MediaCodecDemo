package com.yhr.mediacodecdemo;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.yhr.mediacodecdemo.decode.AudioDecoder;
import com.yhr.mediacodecdemo.decode.VideoDecoder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MediaCodecUtil {

    private static final String TAG = "mediacodec-demo";

    public static void playVideo(Context context, Surface surface) {
        getLatestVideoPath(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull String s) {
                        VideoDecoder videoDecoder = new VideoDecoder(s, surface);
                        videoDecoder.start();

                        AudioDecoder audioDecoder = new AudioDecoder(s);
                        audioDecoder.start();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

//    public void func() {
//        MediaMuxer muxer = new MediaMuxer("temp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        // MediaCodec.getOutputFormat()
//        // MediaExtractor.getTrackFormat()
//        MediaFormat audioFormat = new MediaFormat(...);
//        MediaFormat videoFormat = new MediaFormat(...);
//        int audioTrackIndex = muxer.addTrack(audioFormat);
//        int videoTrackIndex = muxer.addTrack(videoFormat);
//        ByteBuffer inputBuffer = ByteBuffer.allocate(bufferSize);
//        boolean finished = false;
//        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//
//        muxer.start();
//        while(!finished) {
//            // getInputBuffer() will fill the inputBuffer with one frame of encoded
//            // sample from either MediaCodec or MediaExtractor, set isAudioSample to
//            // true when the sample is audio data, set up all the fields of bufferInfo,
//            // and return true if there are no more samples.
//            finished = getInputBuffer(inputBuffer, isAudioSample, bufferInfo);
//            if (!finished) {
//                int currentTrackIndex = isAudioSample ? audioTrackIndex : videoTrackIndex;
//                muxer.writeSampleData(currentTrackIndex, inputBuffer, bufferInfo);
//            }
//        };
//        muxer.stop();
//        muxer.release();
//    }

    /**
     * 获取最新视频的路径
     *
     * @return 路径
     */
    private static Single<String> getLatestVideoPath(Context context) {
        return Single.create((emitter -> {
            Cursor cursor = null;
            try {
                cursor = context
                        .getContentResolver()
                        .query(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                new String[]{MediaStore.MediaColumns.DATA},
                                null,
                                null,
                                MediaStore.MediaColumns.DATE_ADDED + " DESC LIMIT " + "1");
                if (cursor != null && cursor.moveToNext()) {
                    Log.d(TAG, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
                    emitter.onSuccess(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
                } else {
                    emitter.onError(new Exception("获取视频失败"));
                }
            } catch (Exception e) {
                emitter.onError(e);
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }));
    }

}

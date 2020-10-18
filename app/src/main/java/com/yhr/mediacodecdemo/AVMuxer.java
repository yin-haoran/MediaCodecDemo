package com.yhr.mediacodecdemo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;

import com.yhr.mediacodecdemo.encode.VideoEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AVMuxer {

    private static final String TAG = "AVMuxer";

    private MediaExtractor audioExtractor;
    private MediaExtractor videoExtractor;

    private MediaMuxer muxer;
    private int audioTrackIndex;
    private int videoTrackIndex;

    private String audioPath;
    private String videoPath;

    private String outputPath;
    private int outputFormat;

    private CompleteCallback completeCallback;

    private volatile boolean videoMux = false;

    public AVMuxer(String audioPath, String videoPath, String outputPath, int outputFormat) {
        this.audioPath = audioPath;
        this.videoPath = videoPath;
        this.outputPath = outputPath;
        this.outputFormat = outputFormat;
    }

    public void start() {
        if (!init()) {
            Log.d(TAG, "start: 初始化错误");
            return;
        }

        muxer.start();
        Executor executor = Executors.newFixedThreadPool(1);
        executor.execute(this::muxingAV);
    }

    private void muxingAV() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        muxAudio(byteBuffer, bufferInfo);

//        muxVideo(byteBuffer, bufferInfo);
        videoMux = true;
        muxCodedVideo();
        while (videoMux) {}

        Log.d(TAG, "muxingAV: over");
        muxer.stop();
        muxer.release();

        if (completeCallback != null) {
            completeCallback.onComplete();
        }
    }

    /**
     * 解码-》编码-》封装
     */
    private void muxCodedVideo() {
        MediaFormat format = videoExtractor.getTrackFormat(videoExtractor.getSampleTrackIndex());

        String mime = format.getString(MediaFormat.KEY_MIME);
        int width = format.getInteger(MediaFormat.KEY_WIDTH) / 2;
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT) / 2;
        int frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE) / 2;
        int bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 2;
        int iFrameInternal = format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL);

        VideoEncoder encoder = new VideoEncoder(mime, width, height, colorFormat, frameRate, bitRate, iFrameInternal);
        encoder.setExtractor(videoExtractor);
        encoder.setEncodeListener(((byteBuffer, bufferInfo) -> {
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                videoMux = false;
            }
            muxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
        }));

    }

    /**
     * 抽取-》封装
     */
    private void muxVideo(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        while (true) {
            int size = videoExtractor.readSampleData(byteBuffer, 0);
            if (size == -1) {
                break;
            }
            // 注意：MediaCodec.BufferInfo   ---   MediaCodec.BUFFER_FLAG_KEY_FRAME   MediaCodec.BUFFER_FLAG_END_OF_STREAM
            bufferInfo.set(0, size, videoExtractor.getSampleTime(), MediaCodec.BUFFER_FLAG_KEY_FRAME);
            muxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo);
            videoExtractor.advance();
            Log.d(TAG, "muxingAV: mux video");
        }
    }

    private void muxAudio(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        while (true) {
            int size = audioExtractor.readSampleData(byteBuffer, 0);
            if (size == -1) {
                break;
            }

            bufferInfo.set(0, size, audioExtractor.getSampleTime(), 0);
            muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo);
            audioExtractor.advance();
            Log.d(TAG, "muxingAV: mux audio");
        }
    }

    private boolean init() {
        try {
            muxer = new MediaMuxer(outputPath, outputFormat);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(audioPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null || !mime.toLowerCase().startsWith("audio/")) {
                continue;
            }

            audioExtractor.selectTrack(i);
            audioTrackIndex = muxer.addTrack(mediaFormat);
        }

        videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null || !mime.toLowerCase().startsWith("video/")) {
                continue;
            }

            videoExtractor.selectTrack(i);
            videoTrackIndex = muxer.addTrack(mediaFormat);
        }

        // todo 可能没有来源
        return true;
    }

    public void setCompleteCallback(CompleteCallback completeCallback) {
        this.completeCallback = completeCallback;
    }

    @FunctionalInterface
    public interface CompleteCallback {
        void onComplete();
    }

}

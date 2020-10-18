package com.yhr.mediacodecdemo.encode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoder {

    MediaCodec encoder;

    MediaExtractor extractor;

    private String mime;
    private int width;
    private int height;
    private int colorFormat;
    private int frameRate;
    private int bitRate;
    private int iFrameInternal;

    private EncodeListener encodeListener;

    public VideoEncoder(String mime, int width, int height, int colorFormat, int frameRate, int bitRate, int iFrameInternal) {
        this.mime = mime;
        this.width = width;
        this.height = height;
        this.colorFormat = colorFormat;
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.iFrameInternal = iFrameInternal;
    }

    public void start() {
        // 设置MediaCodec异步处理数据
        encoder.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                if (extractor == null) {
                    return;
                }

                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    return;
                }

                inputBuffer.clear();
                int size = extractor.readSampleData(inputBuffer, 0);
                if (size == -1) {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    codec.queueInputBuffer(index, 0, size, extractor.getSampleTime(), 0);
                }

                extractor.advance();
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                if (encodeListener == null) {
                    codec.releaseOutputBuffer(index, false);
                    return;
                }

                ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                if (outputBuffer == null) {
                    return;
                }

                encodeListener.onDataEncoded(outputBuffer, info);
                codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                e.printStackTrace();
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });

        // MediaCodec进入Running
        encoder.start();
    }

    /**
     * 编码器初始化
     */
    private boolean init() {
        MediaFormat format = MediaFormat.createVideoFormat(mime, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInternal);

        try {
            encoder = MediaCodec.createEncoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

     return false;
    }

    public void setExtractor(MediaExtractor extractor) {
        this.extractor = extractor;
    }

    public void setEncodeListener(EncodeListener encodeListener) {
        this.encodeListener = encodeListener;
    }

    @FunctionalInterface
    public static interface EncodeListener {
        void onDataEncoded(@NonNull ByteBuffer byteBuffer, @NonNull MediaCodec.BufferInfo bufferInfo);
    }

}

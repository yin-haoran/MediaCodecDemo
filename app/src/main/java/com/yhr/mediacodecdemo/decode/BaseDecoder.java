package com.yhr.mediacodecdemo.decode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class BaseDecoder implements IDecoder {

    private static final String TAG = "BaseDecoder";

    protected MediaExtractor extractor;
    protected MediaCodec codec;

    private List<BufferEntity> outputBuffers = new LinkedList<>();

    private boolean running;

    private long startTime = -1L;

    @Override
    public void start() {
        boolean initSuccess = init();
        if (!initSuccess) {
            Log.d(TAG, "start: init()失败");
            return;
        }
        setCodecCallback();
        codec.start();

        running = true;
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this);
    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    /**
     * 初始化MediaExtractor、MediaCodec
     */
    public abstract boolean init();

    /**
     * 渲染
     *
     * @param outputBufferIndex buffer下标
     * @param dataAmountInByte  buffer中数据的字节数
     */
    public abstract void render(int outputBufferIndex, int dataAmountInByte);

    @Override
    public void run() {
        while (running) {
            if (outputBuffers.isEmpty()) {
                continue;
            }

            BufferEntity outputBuffer = outputBuffers.remove(0);
            long pts = outputBuffer.bufferInfo.presentationTimeUs / 1000;
            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            }
            long passTime = System.currentTimeMillis() - startTime;
            if (pts > passTime) {
                try {
                    Log.d(TAG, "run: sleep...");
                    Thread.sleep(pts - passTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            render(outputBuffer.index, outputBuffer.bufferInfo.size);
        }
    }

    public void setCodecCallback() {
        // 回调方法在主线程
        codec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    return;
                }
                inputBuffer.clear();
                int bufferSize = extractor.readSampleData(inputBuffer, 0);
                if (bufferSize != -1) {
                    codec.queueInputBuffer(index, 0, bufferSize, extractor.getSampleTime(), 0);
                } else {
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                extractor.advance();
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                outputBuffers.add(new BufferEntity(index, info));
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                e.printStackTrace();
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
    }

    private static class BufferEntity {
        private int index;
        private MediaCodec.BufferInfo bufferInfo;

        public BufferEntity(int index, MediaCodec.BufferInfo bufferInfo) {
            this.index = index;
            this.bufferInfo = bufferInfo;
        }
    }

}

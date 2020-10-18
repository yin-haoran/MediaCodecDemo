package com.yhr.mediacodecdemo.decode;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoder extends BaseDecoder {

    private static final String TAG = "AudioDecoder";

    private AudioTrack audioTrack;

    private String filePath;

    public AudioDecoder(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 初始化MediaExtractor、MediaCodec
     */
    @Override
    public boolean init() {
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime == null || !mime.toLowerCase().startsWith("audio/")) {
                continue;
            }
            extractor.selectTrack(i);

            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();

            int bufferSize = audioTrack.getMinBufferSize(
                    sampleRate,
                    channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            audioTrack = new AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

            audioTrack.play();

            try {
                codec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            codec.configure(mediaFormat, null, null, 0);

            return true;
        }

        return false;
    }

    @Override
    public void render(int outputBufferIndex, int dataAmountInByte) {
        ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
        if (outputBuffer == null) {
            return;
        }

        byte[] bytes = new byte[dataAmountInByte];
        outputBuffer.get(bytes);

        audioTrack.write(bytes, 0, bytes.length);

        codec.releaseOutputBuffer(outputBufferIndex, false);

        Log.d(TAG, "render: ");
    }
}

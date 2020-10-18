package com.yhr.mediacodecdemo.decode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class VideoDecoder extends BaseDecoder {

    private static final String TAG = "VideoDecoder";

    private String filePath;
    private Surface surface;

    public VideoDecoder(String filePath, Surface surface) {
        this.filePath = filePath;
        this.surface = surface;
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
            if (mime == null || !mime.toLowerCase().startsWith("video/")) {
                continue;
            }
            try {
                codec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            codec.configure(mediaFormat, surface, null, 0);

            extractor.selectTrack(i);
            return true;
        }


        return false;
    }

    @Override
    public void render(int outputBufferIndex, int dataAmountInByte) {
        //        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(() -> codec.releaseOutputBuffer(index, true));
        codec.releaseOutputBuffer(outputBufferIndex, true);
        Log.d(TAG, "render: ");
    }
}

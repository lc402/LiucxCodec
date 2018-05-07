package com.camera.b715.liucxcodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by b715 on 18-5-7.
 */

public class LiucxCodec {
    private static final String DEFAULT_MIME_TYPE = "video/avc";
    private String mMimeType = DEFAULT_MIME_TYPE;//can set
    private MediaCodec mCodec;
    private MediaCodecList[] mLocalMachineCodecs;
    private MediaCodecInfo mMediaCodeInfo = null;
    private MediaFormat mMediaFormat = null;
    private int mColorFormat = 0;
    private int mWidth;
    private int mHeight;
    private int mFrameRate = 25;
    private int mCompressRatio = 256;
    private int mBitRate = 720 * 1280 * 3 * 8 * mFrameRate / mCompressRatio;
    private int mIframeInterval = mFrameRate;
    private DataCallback mDataCallback;

    interface DataCallback {
        //put video data to encodec
        int queueInputBuffer(ByteBuffer buffer);
        //get data decodeced
        int dequeueoutputBuffer(byte[] data);
    }

    public void createCodec() throws IOException {
        if(mMediaCodeInfo==null)
            selectCodec();
        if(mColorFormat == 0 && mMediaCodeInfo != null)
            createColorFormat(mMediaCodeInfo);
        if(mMediaFormat == null && mColorFormat != 0)
            createMediaFormat();

        mCodec = MediaCodec.createByCodecName(mMediaCodeInfo.getName());
        mCodec.configure(mMediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

        mCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                ByteBuffer byteBuffer = codec.getInputBuffer(index);
                byteBuffer.clear();
                int len = mDataCallback.queueInputBuffer(byteBuffer);
                codec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0 );
            }

            @Override
            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                ByteBuffer byteBuffer = codec.getOutputBuffer(index);
                if(byteBuffer != null && info.size>0) {
                    byte[] bytes = new byte[info.size];
                    byteBuffer.get(bytes);
                    mDataCallback.dequeueoutputBuffer(bytes);
                    codec.releaseOutputBuffer(index, true);
                }
            }

            @Override
            public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                Log.e("test", e.toString());
            }

            @Override
            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {

            }
        });

        mCodec.start();
    }

    private void createMediaFormat() {
        mMediaFormat = MediaFormat.createVideoFormat(mMimeType, mWidth, mHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIframeInterval);
    }
    private int createColorFormat(MediaCodecInfo codecInfo) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mMimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e("test",
                "couldn't find a good color format for " + codecInfo.getName()
                        + " / " + mMimeType);
        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                return true;
            default:
                return false;
        }
    }

    public MediaCodecInfo selectCodec() {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfos = mediaCodecList.getCodecInfos();
        for (MediaCodecInfo info : mediaCodecInfos) {
            if (!info.isEncoder())
                continue;
            String[] types = info.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mMimeType)) {
                    mMediaCodeInfo = info;
                    return info;
                }
            }

        }
        return null;
    }
}

/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zz.scandemo.zbar.decode;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import com.zz.scandemo.R;
import com.zz.scandemo.CaptureActivity;
import com.zz.scandemo.zbar.utils.PlanarYUVLuminanceSource;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.wechat_qrcode.WeChatQRCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import static android.graphics.Bitmap.createBitmap;


final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();
    private WeChatQRCode mWeChatQRCode;
    private final CaptureActivity activity;
    private File mDetectorPrototxtFile = null;
    private File mDetectorCaffeModelFile = null;
    private File mSuperResolutionPrototxtFile = null;
    private File mSuperResolutionCaffeModelFile = null;
    private boolean running = true;

    DecodeHandler(CaptureActivity activity) {
        this.activity = activity;
        initModelFile();
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);

        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }


    /**
     * 解码
     */

    private void decode(byte[] data, int width, int height) {
        // long start = System.currentTimeMillis();
        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }

        // 宽高也要调整
        int tmp = width;
        width = height;
        height = tmp;
        RectF mCropRect = activity.initCrop();
        String result = null;
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                rotatedData, width, height, (int) mCropRect.left, (int) mCropRect.top, (int) mCropRect.width(), (int) mCropRect.height(), true);

        try {

            Mat mGray = new Mat((int) mCropRect.height(), (int) mCropRect.width(), CvType.CV_8UC1);
            mGray.put(0, 0, source.getMatrix());
            List<String> results = mWeChatQRCode.detectAndDecode(mGray);
            if (results != null && results.size() > 0) {
                Log.i(TAG, results.toString());
                result = results.get(0);
            }

//                result = zBarDecoder.decodeCrop(rotatedData, width, height, mCropRect.left, mCropRect.top, mCropRect.width(), mCropRect.height());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Handler handler = activity.getHandler();
        if (result != null) {
            // long end = System.currentTimeMillis();
            if (handler != null) {
                Message message = Message.obtain(handler,
                        R.id.decode_succeeded, result);
                Bundle bundle = new Bundle();
                bundle.putParcelable("bitmap", source.renderCroppedGreyscaleBitmap());
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }


    private void initModelFile() {
        try {
            // detect.prototxt
            InputStream detectorIs = activity.getResources().openRawResource(R.raw.detect_prototxt);
            File qrcodeDir = activity.getDir("qrcode", Context.MODE_PRIVATE);
            mDetectorPrototxtFile = new File(qrcodeDir, "detect.prototxt");
            FileOutputStream os = new FileOutputStream(mDetectorPrototxtFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (-1 != (bytesRead = detectorIs.read(buffer))) {
                os.write(buffer, 0, bytesRead);
            }
            detectorIs.close();
            os.close();


            // detect.caffemodel
            InputStream detectorCaffeIs = activity.getResources().openRawResource(R.raw.detect_caffemodel);
            mDetectorCaffeModelFile = new File(qrcodeDir, "detect.caffemodel");
            FileOutputStream ios = new FileOutputStream(mDetectorCaffeModelFile);
            byte[] ibuffer = new byte[4096];
            int ibytesRead;
            while ((ibytesRead = detectorCaffeIs.read(ibuffer)) != -1) {
                ios.write(ibuffer, 0, ibytesRead);
            }
            detectorCaffeIs.close();
            ios.close();
            // sr.prototxt
            InputStream srPrototxtIs = activity.getResources().openRawResource(R.raw.sr_prototxt);
            mSuperResolutionPrototxtFile = new File(qrcodeDir, "sr.prototxt");
            FileOutputStream jos = new FileOutputStream(mSuperResolutionPrototxtFile);
            byte[] jbuffer = new byte[4096];
            int jbytesRead;
            while ((jbytesRead = srPrototxtIs.read(jbuffer)) != -1) {
                jos.write(jbuffer, 0, jbytesRead);
            }
            srPrototxtIs.close();
            jos.close();

            // sr.caffemodel
            InputStream srCaffeIs = activity.getResources().openRawResource(R.raw.sr_caffemodel);
            mSuperResolutionCaffeModelFile = new File(qrcodeDir, "sr.caffemodel");
            FileOutputStream kos = new FileOutputStream(mSuperResolutionCaffeModelFile);
            byte[] kbuffer = new byte[4096];
            int kbytesRead;
            while ((kbytesRead = srCaffeIs.read(kbuffer)) != -1) {
                kos.write(kbuffer, 0, kbytesRead);
            }
            srCaffeIs.close();
            kos.close();

            mWeChatQRCode = new WeChatQRCode(
                    mDetectorPrototxtFile.getAbsolutePath(),
                    mDetectorCaffeModelFile.getAbsolutePath(),
                    mSuperResolutionPrototxtFile.getAbsolutePath(),
                    mSuperResolutionCaffeModelFile.getAbsolutePath()
            );

        } catch (Exception e) {
            Log.i("qwe", "----------" + e.toString());
        }
    }

}

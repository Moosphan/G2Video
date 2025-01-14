/*
 *  Copyright (C) 2017 Bilibili
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.moosphon.g2v.selector;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A compress task for {@link com.moosphon.g2v.selector.ImageMediaEntity}
 * @author ChenSL
 */
public class CompressTask {
    public static boolean compress(Context context, final ImageMediaEntity image) {
        return compress(new ImageCompressor(context), image, ImageCompressor.MAX_LIMIT_SIZE_LONG);
    }

    /**
     * @param imageCompressor see {@link ImageCompressor}.
     * @param maxSize the proximate max size for compression
     * @return may be a little bigger than expected for performance.
     */
    public static boolean compress(final ImageCompressor imageCompressor, final ImageMediaEntity image, final long maxSize) {
        if (imageCompressor == null || image == null || maxSize <= 0) {
            return false;
        }
        FutureTask<Boolean> task = MediaExecutor.getInstance().runWorker(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final String path = image.getPath();
                File compressSaveFile = imageCompressor.getCompressOutFile(path);
                File needCompressFile = new File(path);
                if (isFileValid(compressSaveFile)) {
                    image.setCompressPath(compressSaveFile.getAbsolutePath());
                    return true;
                }
                if (!isFileValid(needCompressFile)) {
                    return false;
                } else if (Long.valueOf(image.getSize()) < maxSize) {
                    image.setCompressPath(path);
                    return true;
                } else {
                    try {
                        File result = imageCompressor.compress(needCompressFile, maxSize);
                        boolean suc = isFileValid(result);
                        image.setCompressPath(suc ? result.getAbsolutePath() : null);
                        return suc;
                    } catch (IOException | OutOfMemoryError | NullPointerException | IllegalArgumentException e) {
                        image.setCompressPath(null);
                        Log.d("CompressTask","image compress fail!");
                    }
                }
                return false;
            }
        });
        try {
            return task != null && task.get();
        } catch (InterruptedException | ExecutionException ignore) {
            return false;
        }
    }

    private static boolean isFileValid(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return isFileValid(file);
    }

    static boolean isFileValid(File file) {
        return file != null && file.exists() && file.isFile() && file.length() > 0 && file.canRead();
    }

}

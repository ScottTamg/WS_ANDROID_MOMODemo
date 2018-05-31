package com.wushuangtech.wstechapi.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;

import com.wushuangtech.utils.PviewLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wangzhiguo on 18/3/5.
 */

public class AssertUtils {

    private static final String TAG = "AssertUtils";

    public static String transformToFile(Context mContext, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            PviewLog.e(TAG, "传入的文件名不对 -> " + fileName);
            return null;
        }

        String targetPath;
        File targetFile;
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            targetPath = mContext.getFilesDir().getParent() + "/assertMusic";
        } else {
            File sdRoot = mContext.getExternalFilesDir(null);
            if (sdRoot == null) {
                targetPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/assertMusic";
            } else {
                targetPath = sdRoot.getAbsolutePath() + "/assertMusic";
            }
        }

        if (TextUtils.isEmpty(targetPath)) {
            PviewLog.e(TAG, "获取存储设备路径出错 -> " + targetPath);
            return null;
        }

        File targetDir = new File(targetPath);
        if (!targetDir.exists()) {
            boolean mkdirs = targetDir.mkdirs();
            if (!mkdirs) {
                PviewLog.e(TAG, "创建assertMusic文件夹失败 -> " + targetPath);
                return null;
            }
        }

        InputStream open = null;
        FileOutputStream fos = null;
        try {
            targetFile = new File(targetDir.getAbsolutePath() + File.separator + fileName);
            if (targetFile.exists()) {
                boolean newFile = targetFile.delete();
                if (!newFile) {
                    PviewLog.e(TAG, "删除目标文件失败 -> " + targetFile.getAbsolutePath());
                    return null;
                }
            }

            AssetManager assets = mContext.getAssets();
            open = assets.open(fileName);
            if (open == null) {
                PviewLog.e(TAG, "打开assets流失败 -> " + fileName);
                return null;
            }

            fos = new FileOutputStream(targetFile);
            byte[] buf = new byte[1024];
            while (open.read(buf) != -1) {
                fos.write(buf, 0, buf.length);
                fos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            PviewLog.e(TAG, "发生IO异常 -> " + e.getLocalizedMessage());
            return null;
        } finally {
            if (open != null) {
                try {
                    open.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (targetFile != null && targetFile.exists()) {
            return targetFile.getAbsolutePath();
        } else {
            return null;
        }
    }
}

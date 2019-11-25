package com.jafir.player;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;


public class FileManager {

    public static final String videoFileDir = Environment.getExternalStorageDirectory() + "/jafirplayer/video/";
    public static final String snapshotFileDir = Environment.getExternalStorageDirectory() + "/jafirplayer/snapshot/";

    /**
     * 删除指定目录下文件及目录
     */
    public void deleteFolderFile(String filePath, boolean deleteThisPath) {
        try {

            if (!TextUtils.isEmpty(filePath)) {
                File file = new File(filePath);

                if (file.isDirectory()) {// 处理目录
                    File files[] = file.listFiles();
                    for (File tmpFile : files) {
                        deleteFolderFile(tmpFile.getAbsolutePath(), true);
                    }
                }
                if (deleteThisPath) {
                    //因为直接删除数据库db有问题，所以这里不直接删掉，而是在此之前调用系统的删除方法
                    if (!file.isDirectory() && !file.getParentFile().getName().equals("databases")) {// 如果是文件，删除
                        file.delete();
                    } else {// 目录
                        if (file.list() != null && file.listFiles().length == 0) {// 目录下没有文件或者目录，删除
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

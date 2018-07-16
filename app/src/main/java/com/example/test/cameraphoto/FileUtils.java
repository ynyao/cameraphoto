package com.example.test.cameraphoto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by apple on 2018/7/13.
 */

public class FileUtils {
    /**
     * 把字节数组保存为一个文件
     * @param
     */
    public static File bytes2File(byte[] b, String outputFile) {
        BufferedOutputStream stream = null;
        File file = null;
        try {
            file = new File(outputFile);
            if(!file.getParentFile().exists()){
                boolean mkdirs = file.getParentFile().mkdirs();
            }
            boolean newFile = file.createNewFile();
            FileOutputStream fstream = new FileOutputStream(file);
            stream = new BufferedOutputStream(fstream);
            stream.write(b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return file;
    }
}

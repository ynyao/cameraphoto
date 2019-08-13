package com.example.test.cameraphoto.mtp;

import java.io.Serializable;

/**
 * Created by apple on 2018/3/26.
 */

public class PicInfo implements Serializable{

    private String mThumbnailPath;
    private long mDateCreated;
    private int mThumbPixWidth;
    private int mThumbPixHeight;
    private int mImagePixWidth;
    private int mImagePixHeight;
    private int sequenceNumber;
    private int objectHandler;
    //暂时放图片在相机中的路径
    private String  keyWords;

    private String filename;
    private String mSerialNumber;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getmSerialNumber() {
        return mSerialNumber;
    }

    public void setmSerialNumber(String mSerialNumber) {
        this.mSerialNumber = mSerialNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }


    public String getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(String keyWords) {
        this.keyWords = keyWords;
    }



    public int getObjectHandler() {
        return objectHandler;
    }

    public void setObjectHandler(int objectHandler) {
        this.objectHandler = objectHandler;
    }

    public String getmThumbnailPath() {
        return mThumbnailPath;
    }

    public void setmThumbnailPath(String mThumbnailPath) {
        this.mThumbnailPath = mThumbnailPath;
    }

    public long getmDateCreated() {
        return mDateCreated;
    }

    public void setmDateCreated(long mDateCreated) {
        this.mDateCreated = mDateCreated;
    }

    public int getmThumbPixWidth() {
        return mThumbPixWidth;
    }

    public void setmThumbPixWidth(int mThumbPixWidth) {
        this.mThumbPixWidth = mThumbPixWidth;
    }

    public int getmThumbPixHeight() {
        return mThumbPixHeight;
    }

    public void setmThumbPixHeight(int mThumbPixHeight) {
        this.mThumbPixHeight = mThumbPixHeight;
    }

    public int getmImagePixWidth() {
        return mImagePixWidth;
    }

    public void setmImagePixWidth(int mImagePixWidth) {
        this.mImagePixWidth = mImagePixWidth;
    }

    public int getmImagePixHeight() {
        return mImagePixHeight;
    }

    public void setmImagePixHeight(int mImagePixHeight) {
        this.mImagePixHeight = mImagePixHeight;
    }

    public int getmImagePixDepth() {
        return mImagePixDepth;
    }

    public void setmImagePixDepth(int mImagePixDepth) {
        this.mImagePixDepth = mImagePixDepth;
    }

    private int mImagePixDepth;


    @Override
    public int hashCode() {
        return objectHandler;
    }

    @Override
    public boolean equals(Object obj) {
        PicInfo pic=(PicInfo)obj;
        if(filename.equals(pic.getFilename())){
            return true;
        }else{
            return false;
        }
    }
}

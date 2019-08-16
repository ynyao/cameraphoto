package com.example.test.cameraphoto.mtp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.test.cameraphoto.Constant;
import com.example.test.cameraphoto.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Created by zcy on 2019-08-14.
 */
public abstract class UsbReceiver extends BroadcastReceiver {

    protected static String TAG = "UsbReceiver";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    MtpDevice mMtpDevice;
    UsbManager manager;
    Activity mContext;
    private StringBuilder filePath = new StringBuilder();

    public UsbReceiver(Activity context){
        mContext=context;
        manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (Constant.mtpDevice != null) {
            mMtpDevice = Constant.mtpDevice;
            startPicStrategy();
        } else {
            UsbDevice device = mContext.getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                checkMtpDevice(device, 0);
            } else {
                showToast(mContext, "请重新插拔连接设备");
            }
        }
        registerReceiverMtp();
    }

    @Override
    public void onReceive(Context context, Intent data) {
        switch (data.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                UsbDevice usbDevice = data.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                checkMtpDevice(usbDevice, 1);
                Constant.usbDeviceName = usbDevice.getDeviceName();
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Constant.usbDeviceName = "";
                Constant.mtpDevice = null;
                if (mMtpDevice != null) {
                    mMtpDevice.close();
                }
                break;
            case ACTION_USB_PERMISSION:
                if (data.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    usbDevice = data.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    checkMtpDevice(usbDevice, 2);
                }
                break;
        }
    }

    void registerReceiverMtp() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        mContext.registerReceiver(this, intentFilter);
        isRegister = true;
    }

    boolean isRegister = false;


    protected void showToast(final Context context, final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void checkMtpDevice(UsbDevice usbDevice, int key) {
        if (!manager.hasPermission(usbDevice)) {
            //请求usb设备权限
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(usbDevice, mPermissionIntent);
            Log.d(TAG, "request usb permission");
            return;
        }
        UsbDeviceConnection usbDeviceConnection = manager.openDevice(usbDevice);
        mMtpDevice = new MtpDevice(usbDevice);
        boolean isOpenMtp = mMtpDevice.open(usbDeviceConnection);
        Constant.usbDeviceName = usbDevice.getDeviceName();

        Log.d(TAG, "isOpenMtp===" + isOpenMtp + usbDevice.getDeviceName());
        if (isOpenMtp) {
            Constant.mtpDevice = mMtpDevice;
            startPicStrategy();
        } else {
            showToast(mContext, "与MTP建立连接失败，请重新插入MTP设备" + key);
        }

    }


    protected PicInfo getPicInfo(String deviceSeriNumber, int objectHandle) {
        MtpObjectInfo mtpobj = mMtpDevice.getObjectInfo(objectHandle);
        long dateCreated = mtpobj.getDateCreated();


        byte[] bytes = mMtpDevice.getThumbnail(objectHandle);
        filePath.setLength(0);
        filePath.append(Environment.getExternalStorageDirectory().getAbsolutePath())
                .append(File.separator)
                .append("thumbCache")
                .append(File.separator)
                .append(String.valueOf(dateCreated))
                .append(".jpg");
        File fileJpg = new File(filePath.toString());
        if (!fileJpg.exists() && bytes != null)
            FileUtils.bytes2File(bytes, filePath.toString());

        PicInfo info = new PicInfo();
        info.setObjectHandler(objectHandle);
        //                                    mtpobj.getName()
        info.setFilename(mtpobj.getName());
        info.setmThumbnailPath(fileJpg.getAbsolutePath());
        info.setmDateCreated(dateCreated);
        info.setmImagePixWidth(mtpobj.getImagePixWidth());
        info.setmImagePixHeight(mtpobj.getImagePixHeight());
        info.setmImagePixDepth(mtpobj.getImagePixDepth());
        info.setmThumbPixHeight(mtpobj.getThumbPixHeight());
        info.setmThumbPixWidth(mtpobj.getThumbPixWidth());
        info.setSequenceNumber(mtpobj.getSequenceNumber());
        info.setKeyWords(mtpobj.getKeywords());
        info.setmSerialNumber(deviceSeriNumber);
        return info;
    }

    public void close() {
        if (isRegister) {
            mContext.unregisterReceiver(this);
            isRegister = false;
        }
        if (mMtpDevice != null) {
            //            mMtpDevice.close();
        }
    }

    public abstract void startPicStrategy();

    public interface MTPFile  {
        void onAllFile(List<PicInfo> list);
        void onFileAdded(List<PicInfo> list);
        void onFileDecrease(List<PicInfo> listAll); //文件减少，列举出设备全部的文件
    }
}

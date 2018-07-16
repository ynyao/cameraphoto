package com.example.test.cameraphoto.mtp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.test.cameraphoto.Constant;
import com.example.test.cameraphoto.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static io.reactivex.Flowable.interval;

/**
 * Created by apple on 2018/3/26.
 */

public class MTPService {

    private static String TAG="MTPService";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    Disposable disposable;

    private StringBuilder filePath = new StringBuilder();
    private Context mContext;

    public MTPService(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        mContext = context;
        mAlert = builder.create();
        if(Constant.mtpDevice!=null) {
            mMtpDevice=Constant.mtpDevice;
            startScanPic();
            
        }
        else
           showToast(context,"请重新插拔连接设备");
        registerReceiverMtp();
    }

    private void showToast(Context context, String s) {
        Toast.makeText(context,s,Toast.LENGTH_SHORT).show();
    }

    boolean isRegister=false;

    void registerReceiverMtp() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mtpReceiver, intentFilter);
        isRegister=true;
    }

    UsbDevice mUsbDevice;
    MtpDevice mMtpDevice;

    AlertDialog mAlert;

    BroadcastReceiver mtpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent data) {
            switch (data.getAction()) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    UsbDevice usbDevice = data.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    checkMtpDevice(usbDevice, 1);
                    Constant.usbDeviceName = usbDevice.getDeviceName();
                    //attachedUsb(data);
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Constant.usbDeviceName = "";
                    Constant.mtpDevice=null;
                    if (mMtpDevice != null) {
                        mMtpDevice.close();
                        disposable.dispose();
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
    };

    public void checkMtpDevice(UsbDevice usbDevice, int key) {
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        boolean isOpenMtp;
        if (manager.hasPermission(usbDevice)) {
            UsbDeviceConnection usbDeviceConnection = manager.openDevice(usbDevice);
            mUsbDevice = usbDevice;
            mMtpDevice = new MtpDevice(usbDevice);
            isOpenMtp = mMtpDevice.open(usbDeviceConnection);
            Constant.usbDeviceName = mUsbDevice.getDeviceName();
        } else {
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            manager.requestPermission(usbDevice, mPermissionIntent);
            return;
        }

        Log.d("","isOpenMtp===" + isOpenMtp + usbDevice.getDeviceName());
        if (isOpenMtp) {
            Constant.mtpDevice = mMtpDevice;
            mAlert.hide();
            startScanPic();
        } else {
            //            handleMtpDevice(usbDevice, 3);
            mAlert.setMessage("与MTP建立连接失败，请重新插入MTP设备" + key );
            mAlert.show();
        }

    }

    public void startScanPic(){
        disposable = interval(8, TimeUnit.SECONDS)
                .onBackpressureDrop()
                .map(new Function<Long, List>() {
                    @Override
                    public List apply(Long aLong) throws Exception {
                        Log.d(TAG,"start1===" + aLong);
                        List list = new ArrayList();
                        if (mMtpDevice != null) {
                            MtpDeviceInfo mtpDeviceInfo = mMtpDevice.getDeviceInfo();
                            String deviceSeriNumber = null;
                            if (mtpDeviceInfo != null)
                                deviceSeriNumber = mtpDeviceInfo.getSerialNumber();
                            else
                                deviceSeriNumber = "xx";
                            int[] storageIds = mMtpDevice.getStorageIds();
                            if (storageIds == null) {
                                showToast(mContext,"获取相机存储空间失败");
                                return list;
                            }
                            for (int storageId : storageIds) {
                                int[] objectHandles = mMtpDevice.getObjectHandles(storageId, MtpConstants.FORMAT_EXIF_JPEG, 0);
                                if(objectHandles==null){
                                    showToast(mContext,"获取照片失败");
                                    return list;
                                }
                                for (int objectHandle : objectHandles) {
                                    MtpObjectInfo mtpobj = mMtpDevice.getObjectInfo(objectHandle);
                                    if (mtpobj == null) {
                                        continue;
                                    }
                                    long dateCreated=mtpobj.getDateCreated();


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
                                    //                                        if(Long.toString(mtpobj.getDateCreated()).startsWith("15")){
                                    //                                            mMtpDevice.deleteObject(objectHandle);
                                    //                                        }
                                    list.add(info);
                                }
                            }
                        }
                        return list;
                    }
                }).subscribeOn(Schedulers.io())               //线程调度器,将发送者运行在子线程
                .observeOn(AndroidSchedulers.mainThread())          //接受者运行在主线程
                .subscribe((Consumer<? super List>) mContext);
    }

    public void close() {
        if(isRegister) {
            mContext.unregisterReceiver(mtpReceiver);
            isRegister=false;
        }
        if (mMtpDevice != null) {
//            mMtpDevice.close();
        }
        if (disposable != null)
            disposable.dispose();
    }




}

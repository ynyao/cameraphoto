package com.example.test.cameraphoto.mtp;

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
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.example.test.cameraphoto.Constant;
import com.example.test.cameraphoto.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
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

    private static String TAG = "MTPService";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    Disposable disposable;

    private StringBuilder filePath = new StringBuilder();
    private Context mContext;
    private Fragment mFragment;
    UsbManager manager;
    Set<PicInfo> currentSet = new HashSet<>();

    public MTPService(Fragment fragment) {
        mFragment = fragment;
        mContext = fragment.getActivity();
        manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        if (Constant.mtpDevice != null) {
            mMtpDevice = Constant.mtpDevice;
            startScanPic();
        } else {
            UsbDevice device = fragment.getActivity().getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                checkMtpDevice(device, 0);
            } else {
                showToast(mContext, "请重新插拔连接设备");
            }
        }
        registerReceiverMtp();
    }

    private void showToast(final Context context, final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
            }
        });

    }

    boolean isRegister = false;

    void registerReceiverMtp() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mtpReceiver, intentFilter);
        isRegister = true;
    }

    MtpDevice mMtpDevice;

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
                    Constant.mtpDevice = null;
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
            startScanPic();
        } else {
            showToast(mContext, "与MTP建立连接失败，请重新插入MTP设备" + key);
        }

    }

    public void startScanPic() {
        disposable = interval(8, TimeUnit.SECONDS)
                .onBackpressureDrop()
                .flatMap(new Function<Long, Flowable<List[]>>() {
                    @Override
                    public Flowable<List[]> apply(Long aLong) throws Exception {
                        Log.d(TAG, "start1===" + aLong);
                        List<PicInfo> list = new ArrayList();
                        if (mMtpDevice != null) {
                            MtpDeviceInfo mtpDeviceInfo = mMtpDevice.getDeviceInfo();
                            String deviceSeriNumber = null;
                            if (mtpDeviceInfo != null)
                                deviceSeriNumber = mtpDeviceInfo.getSerialNumber();
                            else
                                deviceSeriNumber = "xx";
                            int[] storageIds = mMtpDevice.getStorageIds();
                            if (storageIds == null) {
                                showToast(mContext, "获取相机存储空间失败");
                                return Flowable.just(new List[]{list,null,null});
                            }
                            for (int storageId : storageIds) {
                                int[] objectHandles = mMtpDevice.getObjectHandles(storageId, MtpConstants.FORMAT_EXIF_JPEG, 0);
                                if (objectHandles == null) {
                                    showToast(mContext, "获取照片失败");
                                    return Flowable.just(new List[]{list,null,null});
                                }
                                for (int objectHandle : objectHandles) {
                                    MtpObjectInfo mtpobj = mMtpDevice.getObjectInfo(objectHandle);
                                    if (mtpobj == null) {
                                        continue;
                                    }
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
                                    //                                        if(Long.toString(mtpobj.getDateCreated()).startsWith("15")){
                                    //                                            mMtpDevice.deleteObject(objectHandle);
                                    //                                        }
                                    list.add(info);
                                }
                            }
                        }
                        sort(list);
                        Log.d(TAG, "list size:" + list.size()+"set size:"+currentSet.size());
                        if(list.size()>currentSet.size()){
                            List mList=new ArrayList();
                            for (int j=0;j<list.size();j++) {
                                PicInfo pic = list.get(j);
                                if (currentSet.add(pic)) {
                                    mList.add(pic);
                                }
                            }
                            return Flowable.just(new List[]{list, mList, null});
                        } else if(list.size()<currentSet.size()){
                            currentSet.clear();
                            currentSet.addAll(list);
                            return Flowable.just(new List[]{list, null, list});
                        }

                        return Flowable.just(new List[]{list, null, null});
                    }
                }).subscribeOn(Schedulers.io())               //线程调度器,将发送者运行在子线程
                .observeOn(AndroidSchedulers.mainThread())          //接受者运行在主线程
                .subscribe(new Consumer<List[]>() {
                    @Override
                    public void accept(List[] lists) throws Exception {
                        ((MTPFile) mFragment).onAllFile(lists[ALL]);
                        if(lists[ADD]!=null){
                            ((MTPFile) mFragment).onFileAdded(lists[ADD]);
                        }
                        if(lists[DECREASE]!=null){
                            ((MTPFile) mFragment).onFileDecrease(lists[DECREASE]);
                        }
                    }
                });
    }


    static final int ALL=0;
    static final int ADD=1;
    static final int DECREASE=2;

    public void close() {
        if (isRegister) {
            mContext.unregisterReceiver(mtpReceiver);
            isRegister = false;
        }
        if (mMtpDevice != null) {
            //            mMtpDevice.close();
        }
        if (disposable != null)
            disposable.dispose();
    }


    public void sort(List<PicInfo> list) {
        Collections.sort(list, new Comparator<PicInfo>() {
            @Override
            public int compare(PicInfo o1, PicInfo o2) {
                return (int) (o1.getmDateCreated() / 1000 - o2.getmDateCreated() / 1000);
            }
        });
    }

    public interface MTPFile  {
        void onAllFile(List<PicInfo> list);
        void onFileAdded(List<PicInfo> list);
        void onFileDecrease(List<PicInfo> listAll); //文件减少，列举出设备全部的文件
    }


}

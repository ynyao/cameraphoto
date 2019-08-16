package com.example.test.cameraphoto.mtp;

import android.mtp.MtpEvent;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zcy on 2019-08-14.
 */
public class MTPService2 extends UsbReceiver {
    private Fragment mFragment;

    public MTPService2(Fragment fragment) {
        super(fragment.getActivity());
        mFragment = fragment;
    }

    CancellationSignal mCancellationSignal = new CancellationSignal();

    /**
     * 尝试了一些MTP设备，mtp api 中的readevent会阻塞，且一些事件无法识别返回
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void startPicStrategy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Log.d(TAG,"start read event");
                        MtpEvent event = mMtpDevice.readEvent(mCancellationSignal);
                        Log.d(TAG,"--eventCode:"+event.getEventCode());
                        int objHandler=event.getObjectHandle();
                        switch (event.getEventCode()) {
                            case MtpEvent.EVENT_OBJECT_ADDED:
                                Log.d(TAG,"EVENT_OBJECT_ADDED,handler"+event.getObjectHandle());
                                List<PicInfo> list=new ArrayList<>();
                                list.add(getPicInfo("xx",objHandler));
                                ((MTPFile) mFragment).onFileAdded(list);
                                break;
                            case MtpEvent.EVENT_OBJECT_REMOVED:
                                Log.d(TAG,"EVENT_OBJECT_REMOVED,handler"+event.getObjectHandle());
                                List<PicInfo> delete=new ArrayList<>();
                                delete.add(getPicInfo("xx",objHandler));
                                ((MTPFile) mFragment).onFileDecrease(delete);
                                break;
                            default:
                                break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }).start();

    }

    @Override
    public void close() {
        super.close();
        mCancellationSignal.cancel();
    }
}

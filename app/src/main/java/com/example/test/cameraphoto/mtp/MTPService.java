package com.example.test.cameraphoto.mtp;

import android.mtp.MtpConstants;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.support.v4.app.Fragment;
import android.util.Log;

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

public class MTPService extends UsbReceiver{


    Disposable disposable;
    private Fragment mFragment;
    Set<PicInfo> currentSet = new HashSet<>();

    public MTPService(Fragment fragment) {
        super(fragment.getActivity());
        mFragment = fragment;
    }


    public void startPicStrategy() {
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
                                    PicInfo info = getPicInfo(deviceSeriNumber, objectHandle);
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

    @Override
    public void close() {
        super.close();
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



}

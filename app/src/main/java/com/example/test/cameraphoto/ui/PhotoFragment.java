package com.example.test.cameraphoto.ui;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.test.cameraphoto.R;
import com.example.test.cameraphoto.mtp.MTPService;
import com.example.test.cameraphoto.mtp.PicInfo;
import com.example.test.cameraphoto.mtp.UsbReceiver;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoFragment extends Fragment implements MTPService.MTPFile {
    private PicAdapter adapter;

    RecyclerView rv;

    private CustomDialog mCustomDialog;
    int columns = 3;
    private TextView tvNoAlbum;
    private TextView tvDelete;
    UsbReceiver mService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_item_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = (RecyclerView) view.findViewById(R.id.list);
        tvDelete=view.findViewById(R.id.tv_delete);
        tvNoAlbum = view.findViewById(R.id.tv_no_album);
        adapter = new PicAdapter(rv, getActivity(),
                mList, columns);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new GridLayoutManager(getActivity(), columns));

        mCustomDialog = new CustomDialog(getActivity(), LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, R.layout.dialog_big_picture,
                R.style.Theme_dialog, Gravity.CENTER, R.style.pop_anim_style);
        adapter.setOnClickListener(new PicAdapter.OnItemClickListener() {
            @Override
            public void onClick(String path) {
                if (path != null) {
                    Transformation transformation = new Transformation() {
                        @Override
                        public Bitmap transform(Bitmap source) {
                            int targetWidth = 1080;
                            if(source.getWidth()==0){
                                return source;
                            }
                            //如果图片小于设置的宽度，则返回原图
                            if(source.getWidth()<targetWidth){
                                return source;
                            }else{
                                //如果图片大小大于等于设置的宽度，则按照设置的宽度比例来缩放
                                double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
                                int targetHeight = (int) (targetWidth * aspectRatio);
                                if (targetHeight != 0 && targetWidth != 0) {
                                    Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
                                    if (result != source) {
                                        source.recycle();
                                    }
                                    return result;
                                } else {
                                    return source;
                                }
                            }
                        }

                        @Override
                        public String key() {
                            return "transformation";
                        }
                    };
                    final ImageView imageView=((ImageView) mCustomDialog.findViewById(R.id.iv_img));
                    final Target target=new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            //加载成功，进行处理
                            imageView.setImageBitmap(bitmap);
                        }
                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        }
                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                            //开始加载
                            imageView.setImageResource(R.drawable.progress_animation);
                        }
                    };
                    imageView.setTag(target);
                    Picasso.get().load(new File(path)).transform(transformation).into(target);
                    mCustomDialog.show();
                }
            }
        });
        adapter.setData(mList);
        mService = new MTPService(this);
    }

    List<PicInfo> mList = new ArrayList<>();


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService.close();
    }


    @Override
    public void onAllFile(List<PicInfo> list) {
        if (list.size() <= 0) {
            tvNoAlbum.setVisibility(View.VISIBLE);
            tvDelete.setVisibility(View.INVISIBLE);
        } else {
            tvNoAlbum.setVisibility(View.INVISIBLE);
//            tvDelete.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileAdded(List<PicInfo> list) {
        for(int i=0;i<list.size();i++){
            mList.add(0,list.get(i));
            adapter.notifyItemRangeInserted(0,1);
        }
        rv.scrollToPosition(0);
    }

    @Override
    public void onFileDecrease(List<PicInfo> list) {
        mList.clear();
        if(list.size()!=1) {
            for (int i = 0; i < list.size(); i++) {
                mList.add(0, list.get(i));
            }
        }else{
            for (Iterator<PicInfo> it = mList.iterator(); it.hasNext(); ) {
                PicInfo info = it.next();
                if (info.getObjectHandler() == list.get(0).getObjectHandler()) {
                    it.remove();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}

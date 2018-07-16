package com.example.test.cameraphoto.ui;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.test.cameraphoto.R;
import com.example.test.cameraphoto.mtp.PicInfo;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoFragment extends Fragment {
    private PicAdapter adapter;

    int listsize = 0;
    RecyclerView rv;

    private CustomDialog mCustomDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_item_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = (RecyclerView) view;

        adapter = new PicAdapter(rv,getActivity(),
                mList, 3);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        mCustomDialog=new CustomDialog(getActivity(), LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,R.layout.dialog_big_picture,
                R.style.Theme_dialog, Gravity.CENTER,R.style.pop_anim_style);
        adapter.setOnClickListener(new PicAdapter.OnItemClickListener() {
            @Override
            public void onClick(String path) {
                if(path!=null){
                    System.out.println(path.toString());
                    Picasso.get().load(new File(path))
                            .resize(1280,720)
                            .placeholder(R.drawable.progress_animation)
                            .error(R.drawable.ic_launcher_background)
                            .into((ImageView) mCustomDialog.findViewById(R.id.iv_img));
                    mCustomDialog.show();
                }
            }
        });

    }

    List<PicInfo> mList=new ArrayList<>();
    //利用一个set 来判断新增加的照片
    Set<PicInfo> set=new HashSet<>();

    public void setData(List<PicInfo> list) {
        int i = 1;
        while (true) {
            Log.d("accept", list.size() + "");
            if (adapter != null && rv != null) {
                sort(list);
                if (listsize < list.size()) {
                    synchronized (PicAdapter.obj) {
                        for (int j = list.size() - 1; j >= 0; j--) {
                            PicInfo pic = list.get(j);
                            if (set.add(pic)) {
                                mList.add(0, pic);
                                adapter.notifyItemRangeInserted(0,1);
                            }
                        }
                        adapter.setData(mList);
                        //adapter.notifyItemRangeInserted(0, list.size() - listsize);
                        rv.scrollToPosition(0);
                    }
                    Log.d("accept","< size:"+list.size());
                } else if (listsize > list.size()) {
                    //TODO 相机中的照片减少 逻辑还需要优化
                    synchronized (PicAdapter.obj) {
                        for (int j = list.size() - 1; j >= 0; j--) {
                            PicInfo pic = list.get(j);
                            set.add(pic);
                        }
                        mList.clear();
                        mList.addAll(list);
                        adapter.setData(mList);
                        adapter.notifyDataSetChanged();
                    }
                    Log.d("accept","> size:"+list.size());
                }
                listsize = list.size();
                break;
            } else {
                //尝试10次后跳出循环 避免出现未响应
                if (++i > 10)
                    return;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        //L.d("cameraphotofragment", "hiddenchanged:" + hidden);
    }


    public void sort(List<PicInfo> list) {
        Collections.sort(list, new Comparator<PicInfo>() {
            @Override
            public int compare(PicInfo o1, PicInfo o2) {
                //System.out.println(o2.getmDateCreated() +" - "+ o1.getmDateCreated());
                return (int) (o2.getmDateCreated()/1000 - o1.getmDateCreated()/1000);
            }
        });
    }


}

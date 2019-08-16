package com.example.test.cameraphoto.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.test.cameraphoto.Constant;
import com.example.test.cameraphoto.R;
import com.example.test.cameraphoto.mtp.PicInfo;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Created by apple on 2018/3/23.
 */

public class PicAdapter extends RecyclerView.Adapter<PicAdapter.ViewHolder> {


    private List<PicInfo> mList = new ArrayList<>();

    Context context;
    RecyclerView mRecyclerView;
    boolean allowDelete=false;
    private Set<PicInfo> deleteSet=new HashSet<>();
    private List<Integer> checkBoxTagList=new ArrayList<>();

    int columns = 1;

    public PicAdapter(RecyclerView rv, Context context, List<PicInfo> list, int columns) {
        mRecyclerView = rv;
        this.columns = columns;
        this.context = context;
        setData(list);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pic, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PicInfo image;
        image = mList.get(position);
//        Picasso.get().load(R.mipmap.icon_list).into(holder.img);
        if(allowDelete){
            holder.cb.setVisibility(View.VISIBLE);
        }else{
            holder.cb.setVisibility(View.INVISIBLE);
        }
        holder.cb.setTag(new Integer(position));
        if (checkBoxTagList != null) {
            holder.cb.setChecked((checkBoxTagList.contains(new Integer(position)) ? true : false));
        } else {
            holder.cb.setChecked(false);
        }
        holder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!checkBoxTagList.contains(holder.cb.getTag())) {
                        checkBoxTagList.add(new Integer(holder.getAdapterPosition()));
                    }
                    Log.d("adapter","check position: "+ position+ " holder position:"+holder.getAdapterPosition());
                    deleteSet.add(mList.get(holder.getAdapterPosition()));
                }else{
                    if (checkBoxTagList.contains(holder.cb.getTag())) {
                        checkBoxTagList.remove(new Integer(holder.getAdapterPosition()));
                    }
                    deleteSet.remove(mList.get(holder.getAdapterPosition()));
                }
            }
        });
        Picasso.get().load(new File(image.getmThumbnailPath())).into(holder.img);
        int startIndex=image.getFilename().length()-10;
        if(startIndex<0){
            startIndex=0;
        }
        holder.tvName.setText(image.getFilename().substring(startIndex));
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onClick(importfile(mList.get(position).getObjectHandler()));
            }
        });
    }


    StringBuilder filePath = new StringBuilder();

    public String importfile(int handler) {
        filePath.setLength(0);
        filePath.append(context.getCacheDir())
                .append(File.separator)
                .append(handler)
                .append(".jpg");
        if(Constant.mtpDevice!=null){
            Constant.mtpDevice.importFile(handler,filePath.toString());
            return filePath.toString();
        }
        return null;
    }


    public void setData(List<PicInfo> list) {
        mList = list;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setAllowDelete(boolean b) {
       allowDelete=b;
       notifyDataSetChanged();
    }

    public void deleteFiles() {
        for (Iterator<PicInfo> it = deleteSet.iterator(); it.hasNext(); ) {
            PicInfo info = it.next();
            if(Constant.mtpDevice.deleteObject(info.getObjectHandler())){
                for (int j=mList.size()-1;j>=0;j--) {
                    PicInfo info1=mList.get(j);
                    if(info.getObjectHandler()==info1.getObjectHandler()){
                        mList.remove(info1);
                        notifyItemRemoved(j);
                        Log.d("adapter","delete position: "+ j);
                        break;
                    }
                }
                it.remove();
            }
        }
        checkBoxTagList.clear();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        View view;
        ImageView img;
        TextView tvName;
        CheckBox cb;

        public ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            tvName=itemView.findViewById(R.id.tv_name);
            cb=itemView.findViewById(R.id.cb_select);
            img = itemView.findViewById(R.id.iv_pic);
            FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            fl.height = getWidth(context) / columns;
            img.setLayoutParams(fl);
        }
    }

    /**
     * 获取屏幕宽
     */
    public static int getWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    OnItemClickListener mListener;

    public void setOnClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    interface OnItemClickListener {
        void onClick(String path);
    }


}

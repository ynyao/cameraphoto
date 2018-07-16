package com.example.test.cameraphoto.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.example.test.cameraphoto.R;


/**
 * Created by apple on 2018/4/9.
 */

public class CustomDialog extends Dialog {
    protected CustomDialog(Context context, int layout, int style) {
        this(context, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT, layout,style, Gravity.CENTER);
    }

    public CustomDialog(Context context, int width, int height
            , int layout, int style, int gravity) {
        this(context,width,height,layout,style,gravity, R.style.pop_anim_style);
    }

    public CustomDialog(Context context, int width, int height,
                        int layout, int style, int gravity, int anim) {
        super(context, style);
        setContentView(layout);
        Window window=getWindow();
        WindowManager.LayoutParams layoutParams=window.getAttributes();
        layoutParams.width=width;
        layoutParams.height=height;
        layoutParams.gravity=gravity;
        window.setAttributes(layoutParams);
        window.setWindowAnimations(anim);

    }
}
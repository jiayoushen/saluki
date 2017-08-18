package com.english_teacher.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.march.dev.utils.GlideUtils;
import com.march.gifmaker.GifMaker;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.english_teacher.base.Contacts.FRAMEANIMATION_WHAT;

/**
 * Created by Administrator on 2017/8/14.
 */

public class FrameAnimation {
    private static FrameAnimation d = null;
    private Handler                     mHandler; // 回调回主线程使用
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    // 用于判断递归退出
    private int i = 0;

    private OnGifPlayOverListener gifPlayOverListener;

    // GIF播放完毕的接口回调
    public interface OnGifPlayOverListener {
        void OnGifPlayOver();
    }

    public static FrameAnimation createFrameAnimation(OnGifPlayOverListener var0) {
        synchronized(Tts.class) {
            if(d == null) {
                d = new FrameAnimation(var0);
            }
        }
        return d;
    }

    public FrameAnimation(OnGifPlayOverListener Listener){
        this.gifPlayOverListener = Listener;
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                if (msg.what == FRAMEANIMATION_WHAT && gifPlayOverListener != null) {
                    gifPlayOverListener.OnGifPlayOver();
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 将URI加载成bitmap并实现GIF动画的方法
     *
     * @param ctx            context对象
     * @param picList        URI地址集合
     * @param bitmaps        存放Glide加载URI成为bitmap对象的集合
     * @param delayTime      gif全帧播完的总时间
     * @param actIsFinishing activity是否销毁
     * @param image          用于加载gif的image对象
     */
    public void animation(final Context ctx, final List picList, final List<Bitmap> bitmaps, final int delayTime, final boolean actIsFinishing, final ImageView image) {//,final FrameAnimationListener FAListener) {
        if (i >= picList.size()) {
            i = 0;
            composeGif(ctx, bitmaps, delayTime, actIsFinishing, image);
            //            if(FAListener!=null)
            //                FAListener.downOver(bitmaps);
            return;
        }
        Glide.with(ctx).load(picList.get(i)).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                i += 1;
                bitmaps.add(resource);
                animation(ctx, picList, bitmaps, delayTime, actIsFinishing, image);//, FAListener);
            }
        });
    }

    /**
     * 具体实现GIF动画的方法
     *
     * @param ctx            context对象
     * @param bitmaps        存放Glide加载URI成为bitmap对象的集合
     * @param delayTime      gif全帧播完的总时间
     * @param actIsFinishing activity是否销毁
     * @param image          用于加载gif的image对象
     */
    private void composeGif(final Context ctx, List<Bitmap> bitmaps, int delayTime, final boolean actIsFinishing, final ImageView image) {
        String absolutePath = new File(Environment.getExternalStorageDirectory()
                , System.currentTimeMillis() + ".gif").getAbsolutePath();
        new GifMaker((delayTime / bitmaps.size()), mExecutorService)
                .makeGifInThread(bitmaps, absolutePath, new GifMaker.OnGifMakerListener() {
                    @Override
                    public void onMakeGifSucceed(String outPath) {
                        if (!actIsFinishing) {
                            GlideUtils.with(ctx, outPath)
                                    // 播放次数
                                    .loopCount(1)
                                    .into(image);
                        }
                    }
                });
        mHandler.sendEmptyMessageDelayed(FRAMEANIMATION_WHAT, delayTime + (delayTime / bitmaps.size()));
    }

    public void animation1(Context ctx, ImageView image) {
        //完全编码实现的动画效果
        AnimationDrawable anim = new AnimationDrawable();
        for (int i = 1; i <= 6; i++) {
            //根据资源名称和目录获取R.java中对应的资源ID
            int id = ctx.getResources().getIdentifier("icon" + i, "mipmap", ctx.getPackageName());
            //根据资源ID获取到Drawable对象
            Drawable drawable = ctx.getResources().getDrawable(id);
            //将此帧添加到AnimationDrawable中
            anim.addFrame(drawable, 150);
        }
        anim.setOneShot(true); //设置为loop
        //        image.setBackgroundDrawable(anim); //将动画设置为ImageView背景
        image.setImageDrawable(anim);
        anim.start();  //开始动画
    }

    public void animation2(Context ctx, ImageView image) {
        //完全编码实现的动画效果
        AnimationDrawable anim = new AnimationDrawable();
        for (int i = 6; i >= 1; i--) {
            //根据资源名称和目录获取R.java中对应的资源ID
            int id = ctx.getResources().getIdentifier("icon" + i, "mipmap", ctx.getPackageName());
            //根据资源ID获取到Drawable对象
            Drawable drawable = ctx.getResources().getDrawable(id);
            //将此帧添加到AnimationDrawable中
            anim.addFrame(drawable, 150);
        }
        anim.setOneShot(true); //设置为loop
        //        image.setBackgroundDrawable(anim); //将动画设置为ImageView背景
        image.setImageDrawable(anim);
        anim.start();  //开始动画
    }
}

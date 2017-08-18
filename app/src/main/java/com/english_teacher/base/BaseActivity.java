package com.english_teacher.base;

import android.app.Activity;

import com.iflytek.sunflower.FlowerCollector;

/**
 * Created by Administrator on 2017/8/15.
 */

public class BaseActivity extends Activity{
    public static final String TAG = BaseActivity.class.getSimpleName();
    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(BaseActivity.this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(BaseActivity.this);
        super.onPause();
    }
}

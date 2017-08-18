package com.english_teacher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.english_teacher.base.BaseActivity;
import com.english_teacher.business.FrameAnimation;
import com.english_teacher.business.Ise;
import com.english_teacher.business.Tts;
import com.english_teacher.permissions.PermissionsActivity;
import com.english_teacher.permissions.PermissionsChecker;
import com.english_teacher.utils.L;

import java.util.ArrayList;
import java.util.List;

import static com.english_teacher.base.Contacts.LOW_SCORE;
import static com.english_teacher.base.Contacts.NOT_STANDARD_ANSWER;
import static com.english_teacher.base.Contacts.NO_ANSWER;
import static com.english_teacher.base.Contacts.VOICE_ORDER;

public class MainActivity extends BaseActivity {
    private ImageView image;
    private List<String> list = new ArrayList<String>();
    protected Activity mActivity;

    /**
     * 动态权限相关
     */
    private static final int REQUEST_CODE = 0; // 请求码
    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private PermissionsChecker mPermissionsChecker; // 权限检测器

    private FrameAnimation fa;
    private Tts tts;
    private Ise ise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        mPermissionsChecker = new PermissionsChecker(this);

        // GIF测试数据
        image = (ImageView) findViewById(R.id.iv_test);
        list.add("http://my.csdn.net/uploads/201208/09/1344497446_1255.png");
        list.add("http://img.my.csdn.net/uploads/201208/09/1344497250_3395.png");
        list.add("http://my.csdn.net/uploads/201208/09/1344497291_6068.png");
        list.add("http://my.csdn.net/uploads/201208/09/1344497296_1903.png");
        list.add("http://my.csdn.net/uploads/201208/09/1344497304_9354.png");
        list.add("http://my.csdn.net/uploads/201208/09/1344497457_3328.png");

        final List<Bitmap> bitmaps = new ArrayList<>();
        final int delayTime = 1000;
        final String text = ((TextView) findViewById(R.id.tv_test)).getText().toString();

        //step1-4
        fa = FrameAnimation.createFrameAnimation(new FrameAnimation.OnGifPlayOverListener() {
            @Override
            public void OnGifPlayOver() {
                Glide.with(mActivity).load(list.get(0)).asBitmap().into(image);
            }
        });
        tts = Tts.createTts(this);
        ise = Ise.createIse(this);
        tts.speech_synthesis(text, new Tts.SpeakBeginListener() {
            @Override
            public void onSpeakBegin() {
                fa.animation(mActivity, list, bitmaps, delayTime, isFinishing(), image);
            }
        }, new Tts.SpeakOverListener() {
            @Override
            public void onSpeakOver() {
                ise.evaluation(text, new Ise.FSMListener() {
                    @Override
                    public void onFiniteStateMachine(int result_state) {
                        L.i("result_state = " + result_state);
                        switch (result_state) {
                            case NO_ANSWER:
                                break;
                            case LOW_SCORE:
                                break;
                            case NOT_STANDARD_ANSWER:
                                break;
                            case VOICE_ORDER:
                                break;
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 缺少权限时, 进入权限配置页面
        if (mPermissionsChecker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        }
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.destroy();
        ise.destroy();
    }
}

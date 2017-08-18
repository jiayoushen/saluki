package com.english_teacher.business;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.english_teacher.R;
import com.english_teacher.utils.L;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.sunflower.FlowerCollector;

import static android.content.Context.MODE_PRIVATE;
import static com.english_teacher.base.Contacts.TTS_WHAT;

/**
 * Created by Administrator on 2017/8/15.
 */

public class Tts {
    private static Tts d = null;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    public static final String PREFER_NAME = "com.iflytek.setting";
    // 默认发音人
    private String voicer = "xiaoyan";
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;
    private Context ctx;
    private Handler mHandler; // 回调回主线程使用

    private SpeakBeginListener speakBeginListener;

    // 合成播放开始的回调
    public interface SpeakBeginListener {
        void onSpeakBegin();
    }

    private SpeakOverListener speakOverListener;

    // 合成播放完毕的回调
    public interface SpeakOverListener {
        void onSpeakOver();
    }

    public static Tts createTts(Context var0) {
        synchronized(Tts.class) {
            if(d == null) {
                d = new Tts(var0);
            }
        }
        return d;
    }

    public Tts(Context ctx) {
        this.ctx = ctx;
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(ctx, mTtsInitListener);
        mSharedPreferences = ctx.getSharedPreferences(PREFER_NAME, MODE_PRIVATE);
        mToast = Toast.makeText(ctx, "", Toast.LENGTH_SHORT);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                if (msg.what == TTS_WHAT && speakOverListener != null) {
                    speakOverListener.onSpeakOver();
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            L.d("InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 语音合成并播放的方法
     * @param text
     * @param speakBeginListener 合成完毕开始播放语音时调用动画的回调
     * @param speakOverListener 播放完毕时调用录音测评的回调
     */
    public void speech_synthesis(String text,SpeakBeginListener speakBeginListener,SpeakOverListener speakOverListener){
        if( null == mTts ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip( "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" );
            return;
        }
        this.speakBeginListener = speakBeginListener;
        this.speakOverListener = speakOverListener;
        // 开始合成
        // 收到onCompleted 回调时，合成结束、生成合成音频
        // 合成的音频格式：只支持pcm格式
        // 移动数据分析，收集开始合成事件
        FlowerCollector.onEvent(ctx, "tts_play");

        // String text = ((TextView) ctx.findViewById(R.id.tv_test)).getText().toString();
        // 设置参数
        setParam();
        int result_code = mTts.startSpeaking(text, mTtsListener);

        if (result_code != ErrorCode.SUCCESS) {

            showTip("语音合成失败,错误码: " + result_code);
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
            if(speakBeginListener != null)
                speakBeginListener.onSpeakBegin();
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(ctx.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(ctx.getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
                sendMessage();
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 参数设置
     *
     * @param //param
     * @return
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            /**
             * TODO 本地合成不设置语速、音调、音量，默认使用语记设置
             * 开发者如需自定义参数，请参考在线合成参数设置
             */
        }
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    public void destroy(){
        if( null != mTts ){
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    /**
     * 延迟100ms转入录音
     */
    private void sendMessage(){
        mHandler.sendEmptyMessageDelayed(TTS_WHAT, 100);
    }
}

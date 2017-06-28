package info.itvincent.quartz.countdown;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * 倒计时器
 * <li>1.支持weakreference的listener，避免内存泄漏</li>
 * <li>2.自修正interval误差，避免跳秒现象</li>
 * <li>3.release()方法可以释放强引用的listener</li>
 *
 * @author zhongyongsheng | http://www.itvincent.info
 */

public class QuartzCountdown {
    public static final long DEFAULT_INTERVAL_MS = 1000L;
    private static final int START = 0;
    private static final int STOP = 1;
    private static final int NEXT = 2;
    private static final int RESET = 3;
    private static final String TAG = "QuartzCountdown";

    private Looper mLooper = Looper.getMainLooper();
    private boolean isStarted;
    private boolean isStopped;
    private long mDurationMs;
    private long mIntervalMs = DEFAULT_INTERVAL_MS;
    private long mStopTimeMs;
    private long mLastNextMs;
    private long mFixedIntervalMs;
    private QuartzCountdownListener mListener;
    private WeakReference<QuartzCountdownListener> mWeakListener;

    public interface QuartzCountdownListener {
        /**
         * 超过intervalMs后，会触发一次
         * @param quartzCountdown
         * @param remainMs 剩余的时长
         */
        void onTic(QuartzCountdown quartzCountdown, long remainMs);

        /**
         * 倒计时停止，或者被stop()时触发
         * @param quartzCountdown
         */
        void onStopped(QuartzCountdown quartzCountdown);
    }

    /**
     * 通过此Builder生成QuartzCountdown
     */
    public static class QuartzCountdownBuilder {
        private QuartzCountdown quartzCountdown;
        public QuartzCountdownBuilder() {
            quartzCountdown = new QuartzCountdown();
        }

        /**
         * 倒计时时长（ms）
         * @param durationMs
         * @return
         */
        public QuartzCountdownBuilder durationMs(long durationMs) {
            quartzCountdown.mDurationMs = durationMs;
            return this;
        }

        /**
         * 每次onTic的间隔（ms）
         * @param intervalMs
         * @return
         */
        public QuartzCountdownBuilder intervalMs(long intervalMs) {
            quartzCountdown.mIntervalMs = intervalMs;
            quartzCountdown.mFixedIntervalMs = intervalMs;
            return this;
        }

        /**
         * 设置listener
         * @param listener
         * @param isWeakRef 是否弱引用
         * @return
         */
        public QuartzCountdownBuilder listener(QuartzCountdownListener listener, boolean isWeakRef) {
            if (isWeakRef) {
                quartzCountdown.mWeakListener = new WeakReference<>(listener);
                quartzCountdown.mListener = null;
            } else {
                quartzCountdown.mListener = listener;
                quartzCountdown.mWeakListener = null;
            }
            return this;
        }

        public QuartzCountdown build() {
            return quartzCountdown;
        }
    }

    private QuartzCountdown() {}

    /**
     * 开始倒计时，reset后可以重新start
     */
    public void start() {
        mHandler.sendMessage(mHandler.obtainMessage(START));
    }

    /**
     * 停止倒计时
     */
    public void stop() {
        stopDelay(0);
    }

    /**
     * 重置，然后可以重新start()
     */
    public void reset() {
        mHandler.sendMessage(mHandler.obtainMessage(RESET));
    }
    /**
     * 释放引用的listener
     */
    public void release() {
        mListener = null;
        mWeakListener = null;
    }

    private void stopDelay(long delayMs) {
        Message message = mHandler.obtainMessage(STOP);
        mHandler.sendMessageDelayed(message, delayMs);
    }

    private void nextDelay(long delayMs) {
        Message message = mHandler.obtainMessage(NEXT);
        mHandler.sendMessageDelayed(message, delayMs);
    }

    private Handler mHandler = new Handler(mLooper) {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case START:
                        if (isStarted) return;
                        isStarted = true;
                        removeMessages(START);

                        if (mDurationMs < mIntervalMs) {
                            stopDelay(mDurationMs);
                            return;
                        }

                        mLastNextMs = SystemClock.elapsedRealtime();
                        mStopTimeMs = mLastNextMs + mDurationMs;
                        nextDelay(0);
                        break;

                    case STOP:
                        if (isStopped) return;
                        isStopped = true;
                        removeMessages(NEXT);

                        processOnStopped();
                        break;

                    case NEXT:
                        if (isStopped) return;
                        removeMessages(NEXT);

                        long nowMs = SystemClock.elapsedRealtime();
                        long stopNowDiffMs = mStopTimeMs - nowMs;
                        if (stopNowDiffMs <= 0) {//已经超过结束时间
                            stop();
                            return;
                        }

                        if (stopNowDiffMs <= mIntervalMs) {//结束时间小于间隔
                            processOnTic(stopNowDiffMs);
                            nextDelay(stopNowDiffMs);
                            return;
                        }

                        mLastNextMs = nowMs;
                        processOnTic(stopNowDiffMs);
                        mFixedIntervalMs = stopNowDiffMs % mIntervalMs;

                        long delay = mFixedIntervalMs - (SystemClock.elapsedRealtime() - mLastNextMs);
                        while (delay <= 0) {//onTic运行得比间隔还长
                            delay += mIntervalMs;
                        }

                        nextDelay(delay);
                        break;
                    case RESET:
                        removeCallbacksAndMessages(null);
                        isStarted = false;
                        isStopped = false;
                        mLastNextMs = 0L;
                        mStopTimeMs = 0L;
                        break;
                    default:
                }
            } catch (Throwable t) {
                Log.e(TAG, "handle message error what:" + msg.what, t);
            }
        }

        private void processOnStopped() {
            if (mListener != null) mListener.onStopped(QuartzCountdown.this);
            if (mWeakListener != null) {
                QuartzCountdownListener l = mWeakListener.get();
                if (l != null)
                    l.onStopped(QuartzCountdown.this);
            }
        }

        private void processOnTic(long stopNowDiffMs) {
            if (mListener != null) mListener.onTic(QuartzCountdown.this, stopNowDiffMs);
            if (mWeakListener != null) {
                QuartzCountdownListener l = mWeakListener.get();
                if (l != null)
                    l.onTic(QuartzCountdown.this, stopNowDiffMs);
            }
            Log.i(TAG, String.format("mFixedIntervalMs:%d,remain:%d", mFixedIntervalMs, stopNowDiffMs));
        }
    };

}

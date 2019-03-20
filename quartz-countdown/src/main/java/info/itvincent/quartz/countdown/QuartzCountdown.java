package info.itvincent.quartz.countdown;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Than the system comes with CountDownTimer made the following enhancements
 * <li>Support WeakReference the listener, to avoid memory leaks. </li>
 * <li>Self-correction heartbeat error, there will not be more and more problems heartbeat interval, to avoid jumping seconds phenomenon. </li>
 * <li>The release () method can release the strongly referenced listener. </li>
 * <li>If you still want to convert the remainMs of onTic() to seconds, you could use math.round (remainMs / 1000f) to get the correct number of seconds</li>
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
         * Will callback when countdown over intervalMs long
         * @param quartzCountdown current QuartzCountdown
         * @param remainMs remain time to countdown
         */
        void onTic(QuartzCountdown quartzCountdown, long remainMs);

        /**
         * Countdown stops, or is triggered by stop ()
         * @param quartzCountdown current QuartzCountdown
         */
        void onStopped(QuartzCountdown quartzCountdown);
    }

    /**
     * Generate QuartzCountdown from this Builder
     */
    public static class QuartzCountdownBuilder {
        private QuartzCountdown quartzCountdown;
        public QuartzCountdownBuilder() {
            quartzCountdown = new QuartzCountdown();
        }

        /**
         * Countdown time（ms）
         * @param durationMs
         * @return
         */
        public QuartzCountdownBuilder durationMs(long durationMs) {
            quartzCountdown.mDurationMs = durationMs;
            return this;
        }

        /**
         * Each time onTic interval（ms）
         * @param intervalMs
         * @return
         */
        public QuartzCountdownBuilder intervalMs(long intervalMs) {
            quartzCountdown.mIntervalMs = intervalMs;
            quartzCountdown.mFixedIntervalMs = intervalMs;
            return this;
        }

        /**
         * set listener
         * @param listener
         * @param isWeakRef true use WeakReference
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
     * start countdown, will invoke listener.onTic().  will stop when countdown to 0;
     */
    public void start() {
        mHandler.sendMessage(mHandler.obtainMessage(START));
    }

    /**
     * stop countdown, and will invoke listener.onStopped()
     */
    public void stop() {
        stopDelay(0);
    }

    /**
     * reset countdown，and you can start() as you want, will not invoke listener.onStopped()
     */
    public void reset() {
        mHandler.sendMessage(mHandler.obtainMessage(RESET));
    }

    /**
     * release the listener
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
                        if (stopNowDiffMs <= 0) {//Has exceeded the end time
                            stop();
                            return;
                        }

                        if (stopNowDiffMs <= mIntervalMs) {//The end time is less than the interval
                            processOnTic(stopNowDiffMs);
                            nextDelay(stopNowDiffMs);
                            return;
                        }

                        mLastNextMs = nowMs;
                        processOnTic(stopNowDiffMs);
                        mFixedIntervalMs = stopNowDiffMs % mIntervalMs;

                        long delay = mFixedIntervalMs - (SystemClock.elapsedRealtime() - mLastNextMs);
                        while (delay <= 0) {//onTic method run over an interval
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

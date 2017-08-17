package info.itvincent.quartz.countdown.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import info.itvincent.quartz.countdown.QuartzCountdown;

public class MainActivity extends AppCompatActivity implements QuartzCountdown.QuartzCountdownListener {

    private TextView mRemainTextView;
    private QuartzCountdown mCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final long duration = 60000;
        mRemainTextView = (TextView) findViewById(R.id.remain);
        mRemainTextView.setText(String.valueOf(duration));
        mCountdown = new QuartzCountdown.QuartzCountdownBuilder()
            .durationMs(duration).intervalMs(1000).listener(this, true).build();
        mCountdown.start();

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCountdown.start();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCountdown.stop();
            }
        });

        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCountdown.reset();
                int roundDuration = Math.round(duration / 1000f);
                mRemainTextView.setText(String.valueOf(roundDuration));
            }
        });
    }

    @Override
    public void onTic(QuartzCountdown quartzCountdown, long remainMs) {
        remainMs = Math.round(remainMs / 1000f);
        mRemainTextView.setText(String.valueOf(remainMs));
        try {
            Thread.sleep(100);//模拟执行延迟
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void onStopped(QuartzCountdown quartzCountdown) {
        mRemainTextView.setText("Stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCountdown.stop();
        mCountdown.release();
    }
}

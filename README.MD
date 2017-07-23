# CountDownTimer 安卓精准倒计时器
Than the system comes with CountDownTimer made the following enhancements:

1. Support WeakReference the listener, to avoid memory leaks.
2. Self-correction heartbeat error, there will not be more and more problems heartbeat interval, to avoid jumping seconds phenomenon.
3. The release () method can release the strongly referenced listener.

## Sample
```
private QuartzCountdown mCountdown;

public void startCountdown() {
    mCountdown = new QuartzCountdown.QuartzCountdownBuilder()
        .durationMs(60000).intervalMs(1000).listener(this, true).build();
    mCountdown.start();//start count down
}

...


@Override
public void onTic(QuartzCountdown quartzCountdown, long remainMs) {
    //invoke every intervalMs
}

@Override
public void onStopped(QuartzCountdown quartzCountdown) {
    //invoke after durationMs or stop()
}
```

## Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

## Step 2. Add the dependency
```
dependencies {
    compile 'com.github.itvincent-git:quartz-countdown:1.0.0'
}
```
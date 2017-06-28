# Android用的倒计时器
比系统自带的CountDownTimer做了以下增强：
1. 支持WeakReference的listener，避免内存泄漏。
2. 自修正心跳误差，不会出现心跳间隔越来越大的问题，避免跳秒现象。
3. release()方法可以释放强引用的listener。

## Sample
```
mCountdown = new QuartzCountdown.QuartzCountdownBuilder()
    .durationMs(60000).intervalMs(1000).listener(this, true).build();
mCountdown.start();//开始计时


...


@Override
public void onTic(QuartzCountdown quartzCountdown, long remainMs) {
    //相隔intervalMs回调一次
}

@Override
public void onStopped(QuartzCountdown quartzCountdown) {
    //durationMs结束或stop()后回调
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
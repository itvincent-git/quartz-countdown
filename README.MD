# Android用的倒计时器
比系统自带的CountDownTimer做了以下增强：
1. 支持WeakReference的listener，避免内存泄漏。
2. 自修正心跳误差，不会出现心跳间隔越来越大的问题，避免跳秒现象。
3. release()方法可以释放强引用的listener。

# Player
### 功能：
> 1、投屏到电视盒子、智能电视（同一wifi下）
>
> 2、截图 (不支持硬解)
>
> 3、录屏（支持m3u8、rtsp格式的直播视频录制）
>
> 4、视频直播相关功能（调音量、亮度、半屏全屏、锁定屏幕）
>
> 5、多种直播源格式的支持（m3u8\rtsp）


# 截图
<img src="screenshot/shot1.png" width="40%"/><img src="screenshot/shot2.png" width="40%"/>
<img src="screenshot/shot3.png" width="80%"/>
<img src="screenshot/shot4.png" width="40%"/><img src="screenshot/shot5.png" width="40%"/>


### PS
1、屏幕录制是参考 简书 [ijkplayer开启rtsp,并且支持录制和截图功能](https://www.jianshu.com/p/8078446fdbe6) 来做的，
当时评论里面留了言，然后很多人找我，问我问题，c++我底层的东西我也不是太懂，就是照着作者的代码，自己重新编译了一下so库。
然后写到了自己的项目里。由于很多人问问题，打算，直接把自己做的项目 写成demo然后分享出来给大家

>评论：
 首先感谢楼主分享思路和代码，很棒。
 原文有些代码 遗漏了，自己手动跑了一遍，这里再分享一下commit（https://github.com/fly7632785/ijkplayer/commit/fdd7c3347bbfafb3bae7b36add2d768e0beafe1b），方便大家自行在自己的项目上进行修改（分支上可以合并也可以手动改）。
 还有几个注意的地方：
 1、只有视频直播才可以录制，点播不行
 2、最好视频开始播放才能录制，需要判断一下
 3、注意只有软解才能录制，一般默认是硬解，需要改一下（硬解也可以播放 但是有时候会莫名其妙出错 还没有还清楚为什么）


2、报错，有很多时候录屏出来报错了，什么都没有，原因有很多：
- 这里测试了下m3u8的直播源可以录制的，rtsp的源也是可以录制，但是有些比如摄像头采集的可能不行

- 有些源的dts不是自增长的，有些源的地址本身是过期的地址，虽然能播，但是录制会失败
```
: Application provided invalid, non monotonically increasing dts to muxer in stream 0: 3031200 >= 0
: Error muxing packet
```

- 有些源的音视频格式不是兼容mp4的，比如有些源的音频是pcm_alaw(视频采集)，就不能直接写到mp4的文件里面，可以改后缀为.mov
```
Could not find tag for codec pcm_alaw in stream #0, codec not currently supported in container MP4

```
- 硬解也可以播放 但是有时候会莫名其妙出错 还没有还清楚为什么（可能跟源有关系）






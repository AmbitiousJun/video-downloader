# "爱优腾芒解析"视频批量下载器

此项目的功能是捕获通过网上各种免费解析接口（类似于：https://****/?url=）中的视频真实下载地址，并将视频下载到本地。

## 技术栈

- Java

- Selenium（模拟真实浏览器环境，访问接口解析地址）

- BrowserBomProxy（代理模拟浏览器的 `http` 请求，捕获视频下载地址）

## 功能

- 目前只适配了TX、MG、QY（其他平台等我需要了再来写）

- 下载器目前支持 4 种下载方式：

  - 单线程下载 mp4、m3u8
  
  - 多线程下载 mp4、m3u8

  > 有的解析接口解析出来的是 m3u8 文件，但是里边的 ts 链接都不是直链，需要通过 Location 请求头来进一步获取真实下载地址，这种方式我暂时没有处理，目前只发现 TX 的 720p m3u8 会出现这种情况

- v2 版本支持下载视频网站的会员视频（需要提前设置好 Cookie）

  - 目前只支持奇艺；优酷的话视频都被加密处理了，我不会搞，直接放弃，其他两个网站等我有钱开会员再说

## ⚠️ 注意事项

1. 请先确保你的电脑具有 java 运行环境（JRE）
2. 如果要使用解析功能，请先自行安装 [ChromeDriver](https://www.selenium.dev/documentation/webdriver/getting_started/install_drivers/#quick-reference) 版本号需要与电脑上浏览器的版本号对应上
3. 如果要使用 ffmpeg 进行转码，请先自行安装 [ffmpeg](https://ffmpeg.org/)
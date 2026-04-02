# Debug Calendar
## 2026-04-02
### 1.检测不到目标
修改了模板匹配系统的以下内容：
- 改进了模板加载路径，添加多路径尝试加载逻辑
- 降低了匹配阈值从 0.8 到 0.7，提高检测灵敏度
- 在 TmpMatcher 和 MultiTmpMatcher 中添加了对比度增强，优化匹配算法
- 添加了异常处理，确保程序不会因模板加载失败而崩溃
### 2.检测不到目标
修改了模板加载机制：
- 将模板图像从 Java 包目录移动到 Android 资源目录 (res/raw)
- 修改 TmpMatcher 和 MultiTmpMatcher 类，使用 Android 资源加载机制
- 更新 TmpMatcherTestOpMode 类，传递 Context 参数给匹配器
- 添加了从 Android 资源加载模板的方法，使用 Bitmap 到 Mat 的转换
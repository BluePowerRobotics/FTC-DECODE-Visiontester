# TM 文件夹程序测试方法与故障排除

本文档详细说明了 FTC 机器人项目中 TM 文件夹内模板匹配程序的测试方法，以及试验失败时的可能原因和补救方法。

## 一、测试环境准备

### 1. 硬件准备
- Control Hub 或 Robot Controller
- 摄像头（内置或外部 Webcam）
- 测试目标（紫色和绿色小球）
- 电源供应

### 2. 软件准备
- Android Studio 最新版本
- FTC SDK
- EasyOpenCV 库
- 模板图像（位于 `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/Vision/TM/template/`）

### 3. 模板图像准备
确保以下模板图像存在：
- `purpleball_template_1.jpg`
- `purpleball_template_2.jpg`
- `purpleball_template_3.jpg`
- `greenball_template_1.jpg`
- `greenball_template_2.jpg`
- `greenball_template_3.jpg`

## 二、测试方法

### 1. 单元测试

**目的**：验证核心功能是否正常工作

**步骤**：
1. 创建测试类 `TmpMatcherTest.java`
2. 测试单模板匹配功能
3. 测试多模板匹配功能
4. 验证跟踪对象管理

**示例代码**：
```java
import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import static org.junit.Assert.*;

public class TmpMatcherTest {
    
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    @Test
    public void testSingleTemplateMatching() {
        // 加载测试图像
        Mat testFrame = Imgcodecs.imread("test_images/test_frame.jpg");
        assertNotNull("Test frame not loaded", testFrame);
        
        // 创建匹配器
        TmpMatcher matcher = new TmpMatcher("purpleball_template_1.jpg");
        
        // 处理帧
        matcher.processFrame(testFrame);
        
        // 验证结果
        List<TmpMatcher.TrackedObject> objects = matcher.getTrackedObjects();
        assertNotNull("Objects list should not be null", objects);
        
        // 释放资源
        testFrame.release();
    }
    
    @Test
    public void testMultiTemplateMatching() {
        Mat testFrame = Imgcodecs.imread("test_images/test_frame.jpg");
        assertNotNull("Test frame not loaded", testFrame);
        
        List<String> templates = Arrays.asList(
            "purpleball_template_1.jpg",
            "greenball_template_1.jpg"
        );
        
        MultiTmpMatcher matcher = new MultiTmpMatcher(templates);
        matcher.processFrame(testFrame);
        
        List<MultiTmpMatcher.TrackedObject> objects = matcher.getTrackedObjects();
        assertNotNull("Objects list should not be null", objects);
        
        testFrame.release();
    }
}
```

### 2. 实时可视化测试

**目的**：通过摄像头实时测试检测效果

**步骤**：
1. 运行 `TmpMatcherTestOpMode`
2. 观察摄像头画面中的检测框
3. 验证目标识别准确性
4. 测试不同距离和角度下的检测性能

**操作流程**：
1. 将机器人连接到 Driver Station
2. 在 Driver Station 上选择并运行 `TmpMatcher Test OpMode`
3. 按下 A 按钮开始处理
4. 将紫色和绿色小球放在摄像头视野内
5. 观察 Driver Station 上的检测结果
6. 移动小球，测试跟踪稳定性

### 3. 性能测试

**目的**：测试处理速度和资源占用

**步骤**：
1. 创建性能测试类
2. 测量处理时间
3. 计算帧率
4. 评估资源占用

**示例代码**：
```java
public class PerformanceTest {
    public void runPerformanceTest() {
        MultiTmpMatcher matcher = new MultiTmpMatcher(Arrays.asList(
            "purpleball_template_1.jpg", 
            "greenball_template_1.jpg"
        ));
        
        Mat testFrame = Imgcodecs.imread("test_images/test_frame.jpg");
        
        // 预热
        for (int i = 0; i < 10; i++) {
            matcher.processFrame(testFrame);
        }
        
        // 性能测试
        long startTime = System.currentTimeMillis();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            matcher.processFrame(testFrame);
        }
        
        long endTime = System.currentTimeMillis();
        double avgTime = (endTime - startTime) / (double) iterations;
        
        System.out.println("Average processing time: " + avgTime + " ms");
        System.out.println("FPS: " + (1000.0 / avgTime));
        
        testFrame.release();
    }
}
```

### 4. 准确率测试

**目的**：测试检测准确率和稳定性

**步骤**：
1. 准备测试数据集
2. 运行检测并记录结果
3. 与标注数据比较
4. 计算准确率

**示例代码**：
```java
public class AccuracyTest {
    public void runAccuracyTest() {
        MultiTmpMatcher matcher = new MultiTmpMatcher(Arrays.asList(
            "purpleball_template_1.jpg", 
            "greenball_template_1.jpg"
        ));
        
        int totalTests = 0;
        int correctDetections = 0;
        
        // 测试图像列表
        List<String> testImages = Arrays.asList(
            "test_images/image1.jpg",
            "test_images/image2.jpg",
            "test_images/image3.jpg"
        );
        
        for (String imagePath : testImages) {
            Mat frame = Imgcodecs.imread(imagePath);
            matcher.processFrame(frame);
            
            List<MultiTmpMatcher.TrackedObject> objects = matcher.getTrackedObjects();
            
            // 根据标注验证检测结果
            if (verifyDetection(objects, imagePath)) {
                correctDetections++;
            }
            
            totalTests++;
            frame.release();
        }
        
        double accuracy = (double) correctDetections / totalTests * 100;
        System.out.println("Accuracy: " + accuracy + "%");
    }
    
    private boolean verifyDetection(List<MultiTmpMatcher.TrackedObject> objects, 
                                    String imagePath) {
        // 实现验证逻辑
        // 可以与标注数据比较位置、类型等
        return !objects.isEmpty();
    }
}
```

## 三、常见故障及解决方案

### 1. 无法检测到目标

**可能原因**：
- 模板图像不合适
- 匹配阈值过高
- 缩放范围不合适
- 摄像头焦距未调整
- 光照条件不佳

**解决方案**：
- 优化模板图像：选择高对比度、特征明显的模板
- 降低 `MATCH_THRESHOLD` 到 0.7-0.75
- 扩大缩放范围（`SCALE_MIN: 0.3`, `SCALE_MAX: 2.5`）
- 调整摄像头焦距，确保图像清晰
- 改善光照条件，避免过暗或过亮

### 2. 误检过多

**可能原因**：
- 匹配阈值过低
- 模板特征不明显
- 背景干扰
- 模板与背景相似

**解决方案**：
- 提高 `MATCH_THRESHOLD` 到 0.85-0.9
- 优化模板图像，突出目标特征
- 降低 `NMS_IOU_THRESHOLD` 到 0.2
- 增加背景过滤步骤
- 使用多个模板提高识别准确性

### 3. 检测速度慢

**可能原因**：
- 缩放步长过小
- 模板数量过多
- 图像分辨率过高
- 设备性能限制

**解决方案**：
- 增大 `SCALE_STEP` 到 0.3-0.4
- 减少模板数量，只使用最有效的模板
- 降低摄像头分辨率（640x480 或 320x240）
- 优化代码，减少不必要的计算
- 考虑使用更强大的硬件

### 4. 目标跟踪不稳定

**可能原因**：
- 目标移动过快
- 丢失帧数设置不合理
- IOU 阈值过低
- 图像模糊

**解决方案**：
- 提高 `MAX_MISSING_FRAMES` 到 7-10
- 提高关联 IOU 阈值到 0.15-0.2
- 优化 `predict()` 方法实现简单的运动预测
- 确保摄像头对焦良好，图像清晰
- 减少目标移动速度，测试跟踪稳定性

### 5. 多模板混淆

**可能原因**：
- 模板之间相似度过高
- 匹配阈值设置不当
- 颜色相似导致混淆

**解决方案**：
- 确保模板之间有明显差异
- 为不同类型模板设置不同的匹配阈值
- 使用颜色预处理增强区分度
- 增加模板数量，覆盖不同角度和光照条件
- 考虑结合颜色过滤提高区分度

### 6. 程序崩溃或异常

**可能原因**：
- OpenCV 库未正确加载
- 模板图像路径错误
- 内存不足
- 相机权限问题

**解决方案**：
- 确保 EasyOpenCV 库正确添加到项目中
- 检查模板图像路径是否正确
- 减少同时处理的模板数量
- 确保应用有相机使用权限
- 添加异常处理代码，避免崩溃

## 四、参数调优指南

### 1. 匹配阈值（MATCH_THRESHOLD）

**默认值**：0.8

**调整建议**：
- **提高阈值（0.85-0.95）**：减少误检，但可能漏检
- **降低阈值（0.7-0.8）**：增加检测率，但可能增加误检

**调优方法**：
```java
// 在 TmpMatcher.java 和 MultiTmpMatcher.java 中修改
private static final double MATCH_THRESHOLD = 0.85;
```

### 2. NMS IOU 阈值（NMS_IOU_THRESHOLD）

**默认值**：0.3

**调整建议**：
- **提高阈值（0.4-0.5）**：保留更多重叠检测
- **降低阈值（0.2-0.3）**：更严格地消除重叠

### 3. 缩放范围（SCALE_MIN, SCALE_MAX）

**默认值**：0.5 - 2.0

**调整建议**：
- **已知目标大小范围**：根据实际目标调整
- **目标较小**：降低 SCALE_MIN 到 0.3
- **目标较大**：提高 SCALE_MAX 到 3.0

### 4. 最大丢失帧数（MAX_MISSING_FRAMES）

**默认值**：5

**调整建议**：
- **快速移动目标**：降低到 3-4
- **稳定目标**：提高到 7-10

## 五、测试结果分析

### 1. 成功指标
- 能够稳定检测到紫色和绿色小球
- 检测框准确包围目标
- 跟踪目标移动流畅
- 处理速度满足实时需求（>15 FPS）
- 误检率低于 20%

### 2. 失败指标
- 无法检测到目标
- 检测框与目标偏差较大
- 跟踪目标时丢失频繁
- 处理速度过慢（<5 FPS）
- 误检率高于 50%

### 3. 改进方向
- 根据测试结果调整参数
- 优化模板图像
- 改进跟踪算法
- 增加颜色过滤等预处理步骤
- 考虑使用深度学习方法提高准确性

## 六、测试报告模板

### 测试环境
- 硬件：Control Hub v2 / Webcam
- 软件：FTC SDK v11.0.0 / EasyOpenCV v1.5.1
- 模板：purpleball_template_1.jpg, greenball_template_1.jpg
- 测试场景：室内光照 / 室外光照

### 测试结果
| 测试项 | 预期结果 | 实际结果 | 状态 | 备注 |
|--------|----------|----------|------|------|
| 单模板检测 | 检测到紫色小球 | ✓ | 通过 | 准确 |
| 单模板检测 | 检测到绿色小球 | ✓ | 通过 | 准确 |
| 多模板检测 | 同时检测两种颜色 | ✓ | 通过 | 准确 |
| 目标跟踪 | 稳定跟踪移动目标 | ✓ | 通过 | 偶有丢失 |
| 处理速度 | >15 FPS | 20 FPS | 通过 | 良好 |
| 误检率 | <20% | 15% | 通过 | 可接受 |

### 问题与解决方案
1. **问题**：远距离检测困难
   **解决方案**：降低匹配阈值到 0.75，扩大缩放范围

2. **问题**：光照变化影响检测
   **解决方案**：增加多个不同光照条件下的模板

3. **问题**：处理速度在多模板时下降
   **解决方案**：减少模板数量，增大缩放步长

### 结论
- 模板匹配系统基本满足需求
- 建议在实际比赛前进行更多场景测试
- 参数调整后可达到预期性能

## 七、总结

通过本文档的测试方法和故障排除指南，您应该能够：
1. 系统地测试 TM 文件夹中的模板匹配程序
2. 识别并解决常见的检测问题
3. 优化参数以获得最佳性能
4. 为比赛做好充分准备

记住，模板匹配的性能高度依赖于模板质量、环境条件和参数设置。通过持续测试和调整，您可以获得可靠的目标检测和跟踪能力，为机器人在比赛中的表现提供有力支持。
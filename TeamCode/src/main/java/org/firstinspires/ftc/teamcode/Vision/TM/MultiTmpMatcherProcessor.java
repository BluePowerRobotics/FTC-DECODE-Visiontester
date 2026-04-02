package org.firstinspires.ftc.teamcode.Vision.TM;

import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Mat;

import java.util.List;

public class MultiTmpMatcherProcessor implements VisionProcessor {
    private final MultiTmpMatcher multiTmpMatcher;
    
    public MultiTmpMatcherProcessor(String templateName) {
        this.multiTmpMatcher = new MultiTmpMatcher(templateName);
    }
    
    public MultiTmpMatcherProcessor(List<String> templateNames) {
        this.multiTmpMatcher = new MultiTmpMatcher(templateNames);
    }
    
    @Override
    public void init(int width, int height, int rotation) {
        // 初始化代码（如果需要）
    }
    
    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        multiTmpMatcher.processFrame(frame);
        return multiTmpMatcher.getTrackedObjects();
    }
    
    @Override
    public void onDrawFrame(Mat canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        // 绘制代码已在MultiTmpMatcher.processFrame中处理
    }
    
    public MultiTmpMatcher getMultiTmpMatcher() {
        return multiTmpMatcher;
    }
}

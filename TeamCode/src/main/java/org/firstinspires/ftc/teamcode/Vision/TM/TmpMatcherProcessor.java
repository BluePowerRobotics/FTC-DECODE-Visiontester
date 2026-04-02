package org.firstinspires.ftc.teamcode.Vision.TM;

import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Mat;

public class TmpMatcherProcessor implements VisionProcessor {
    private final TmpMatcher tmpMatcher;
    
    public TmpMatcherProcessor(String templateName) {
        this.tmpMatcher = new TmpMatcher(templateName);
    }
    
    @Override
    public void init(int width, int height, int rotation) {
        // 初始化代码（如果需要）
    }
    
    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        tmpMatcher.processFrame(frame);
        return tmpMatcher.getTrackedObjects();
    }
    
    @Override
    public void onDrawFrame(Mat canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        // 绘制代码已在TmpMatcher.processFrame中处理
    }
    
    public TmpMatcher getTmpMatcher() {
        return tmpMatcher;
    }
}

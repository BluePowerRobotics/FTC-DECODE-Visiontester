package org.firstinspires.ftc.teamcode.Vision.TM;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MultiTmpMatcher {
    private static final int MAX_MISSING_FRAMES = 5;
    private static final double NMS_IOU_THRESHOLD = 0.3;
    private static final double MATCH_THRESHOLD = 0.7;
    private static final double SCALE_MIN = 0.5;
    private static final double SCALE_MAX = 2.0;
    private static final double SCALE_STEP = 0.2;
    
    private List<Mat> templates;
    private List<String> templateNames;
    private List<TrackedObject> trackedObjects;
    private int nextObjectId;
    private Context context;
    
    public MultiTmpMatcher(Context context, String templateName) {
        this.context = context;
        this.trackedObjects = new ArrayList<>();
        this.nextObjectId = 0;
        this.templates = new ArrayList<>();
        this.templateNames = new ArrayList<>();
        
        addTemplate(templateName);
    }
    
    public MultiTmpMatcher(Context context, List<String> templateNames) {
        this.context = context;
        this.trackedObjects = new ArrayList<>();
        this.nextObjectId = 0;
        this.templates = new ArrayList<>();
        this.templateNames = new ArrayList<>();
        
        for (String name : templateNames) {
            addTemplate(name);
        }
    }
    
    private void addTemplate(String templateName) {
        try {
            Mat template = loadTemplateFromResources(templateName);
            
            if (template.empty()) {
                System.err.println("Failed to load template: " + templateName);
                return;
            }
            templates.add(template);
            this.templateNames.add(templateName);
        } catch (Exception e) {
            System.err.println("Error loading template: " + e.getMessage());
        }
    }
    
    // 从 Android 资源加载模板
    private Mat loadTemplateFromResources(String templateName) throws IOException {
        // 移除文件扩展名，获取资源 ID
        String resourceName = templateName.replace(".jpg", "");
        int resourceId = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
        
        if (resourceId == 0) {
            System.err.println("Resource not found: " + resourceName);
            return new Mat();
        }
        
        // 从资源中读取图像
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        
        // 将 Bitmap 转换为 OpenCV Mat
        Mat mat = new Mat();
        org.opencv.android.Utils.bitmapToMat(bitmap, mat);
        bitmap.recycle();
        
        return mat;
    }
    
    public void processFrame(Mat frame) {
        if (templates.isEmpty()) {
            return;
        }
        
        List<DetectionResult> detections = detect(frame);
        
        associateDetectionsWithTrackers(detections);
        
        updateTrackers();
        
        List<TrackedObject> filteredObjects = applyNMS();
        
        drawResults(frame, filteredObjects);
    }
    
    private List<DetectionResult> detect(Mat frame) {
        List<DetectionResult> detections = new ArrayList<>();
        
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        
        // 增加对比度，提高匹配准确性
        Imgproc.equalizeHist(grayFrame, grayFrame);
        
        for (int templateIdx = 0; templateIdx < templates.size(); templateIdx++) {
            Mat template = templates.get(templateIdx);
            String templateName = templateNames.get(templateIdx);
            
            for (double scale = SCALE_MIN; scale <= SCALE_MAX; scale += SCALE_STEP) {
                Mat resizedTemplate = new Mat();
                Imgproc.resize(template, resizedTemplate, new Size(), scale, scale);
                
                if (resizedTemplate.cols() > frame.cols() || resizedTemplate.rows() > frame.rows()) {
                    resizedTemplate.release();
                    continue;
                }
                
                Mat grayTemplate = new Mat();
                Imgproc.cvtColor(resizedTemplate, grayTemplate, Imgproc.COLOR_BGR2GRAY);
                
                // 增加模板对比度
                Imgproc.equalizeHist(grayTemplate, grayTemplate);
                
                int resultCols = grayFrame.cols() - grayTemplate.cols() + 1;
                int resultRows = grayFrame.rows() - grayTemplate.rows() + 1;
                Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);
                
                Imgproc.matchTemplate(grayFrame, grayTemplate, result, Imgproc.TM_CCOEFF_NORMED);
                
                Mat mask = new Mat();
                Imgproc.threshold(result, mask, MATCH_THRESHOLD, 1.0, Imgproc.THRESH_BINARY);
                
                while (true) {
                    Core.MinMaxLocResult mmr = Core.minMaxLoc(mask);
                    if (mmr.maxVal > 0) {
                        Point matchLoc = mmr.maxLoc;
                        Rect rect = new Rect((int)matchLoc.x, (int)matchLoc.y, grayTemplate.cols(), grayTemplate.rows());
                        detections.add(new DetectionResult(rect, templateIdx, templateName));
                        
                        Imgproc.rectangle(mask, matchLoc, 
                            new Point(matchLoc.x + grayTemplate.cols(), matchLoc.y + grayTemplate.rows()), 
                            new Scalar(0), -1);
                    } else {
                        break;
                    }
                }
                
                resizedTemplate.release();
                grayTemplate.release();
                result.release();
                mask.release();
            }
        }
        
        grayFrame.release();
        
        return detections;
    }
    
    private void associateDetectionsWithTrackers(List<DetectionResult> detections) {
        if (trackedObjects.isEmpty()) {
            for (DetectionResult detection : detections) {
                TrackedObject obj = new TrackedObject(detection.rect, nextObjectId++, detection.templateIdx, detection.templateName);
                trackedObjects.add(obj);
            }
            return;
        }
        
        double[][] iouMatrix = new double[trackedObjects.size()][detections.size()];
        for (int i = 0; i < trackedObjects.size(); i++) {
            for (int j = 0; j < detections.size(); j++) {
                iouMatrix[i][j] = calculateIOU(trackedObjects.get(i).getRect(), detections.get(j).rect);
            }
        }
        
        Set<Integer> matchedTrackers = new HashSet<>();
        Set<Integer> matchedDetections = new HashSet<>();
        
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < trackedObjects.size(); i++) {
            for (int j = 0; j < detections.size(); j++) {
                if (iouMatrix[i][j] > 0.1) {
                    matches.add(new Match(i, j, iouMatrix[i][j]));
                }
            }
        }
        
        matches.sort((m1, m2) -> Double.compare(m2.iou, m1.iou));
        
        for (Match match : matches) {
            if (!matchedTrackers.contains(match.trackerIdx) && !matchedDetections.contains(match.detectionIdx)) {
                TrackedObject obj = trackedObjects.get(match.trackerIdx);
                DetectionResult detection = detections.get(match.detectionIdx);
                obj.update(detection.rect, detection.templateIdx, detection.templateName);
                matchedTrackers.add(match.trackerIdx);
                matchedDetections.add(match.detectionIdx);
            }
        }
        
        for (int i = 0; i < trackedObjects.size(); i++) {
            if (!matchedTrackers.contains(i)) {
                TrackedObject obj = trackedObjects.get(i);
                obj.incrementMissingFrames();
            }
        }
        
        for (int j = 0; j < detections.size(); j++) {
            if (!matchedDetections.contains(j)) {
                DetectionResult detection = detections.get(j);
                TrackedObject obj = new TrackedObject(detection.rect, nextObjectId++, detection.templateIdx, detection.templateName);
                trackedObjects.add(obj);
            }
        }
        
        trackedObjects.removeIf(obj -> obj.getMissingFrames() > MAX_MISSING_FRAMES);
    }
    
    private void updateTrackers() {
        for (TrackedObject obj : trackedObjects) {
            obj.predict();
        }
    }
    
    private List<TrackedObject> applyNMS() {
        List<TrackedObject> result = new ArrayList<>();
        List<TrackedObject> objects = new ArrayList<>(trackedObjects);
        
        objects.sort((o1, o2) -> Double.compare(o2.getRect().area(), o1.getRect().area()));
        
        while (!objects.isEmpty()) {
            TrackedObject current = objects.remove(0);
            result.add(current);
            
            objects.removeIf(obj -> calculateIOU(current.getRect(), obj.getRect()) > NMS_IOU_THRESHOLD);
        }
        
        return result;
    }
    
    private void drawResults(Mat frame, List<TrackedObject> objects) {
        for (TrackedObject obj : objects) {
            Rect rect = obj.getRect();
            Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), 
                             new Scalar(0, 255, 0), 2);
            Imgproc.putText(frame, "ID: " + obj.getId(), new Point(rect.x, rect.y - 10), 
                          Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
            Imgproc.putText(frame, "Type: " + obj.getTemplateName(), 
                          new Point(rect.x, rect.y + rect.height + 20), 
                          Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
            Imgproc.putText(frame, "Size: " + rect.width + "x" + rect.height, 
                          new Point(rect.x, rect.y + rect.height + 35), 
                          Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
        }
    }
    
    private double calculateIOU(Rect a, Rect b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);
        
        int intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        int union = (int)(a.area() + b.area() - intersection);
        
        return union > 0 ? (double) intersection / union : 0;
    }
    
    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }
    
    // 内部类：VisionProcessor实现，用于与VisionPortal集成
    public static class MultiTmpMatcherProcessor implements org.firstinspires.ftc.vision.VisionProcessor {
        private MultiTmpMatcher matcher;
        
        public MultiTmpMatcherProcessor(Context context, String templateName) {
            this.matcher = new MultiTmpMatcher(context, templateName);
        }
        
        public MultiTmpMatcherProcessor(Context context, List<String> templateNames) {
            this.matcher = new MultiTmpMatcher(context, templateNames);
        }


        @Override
        public void init(int i, int i1, CameraCalibration cameraCalibration) {

        }

        public Object processFrame(org.opencv.core.Mat frame, long captureTimeNanos) {
            // 处理每一帧图像
            matcher.processFrame(frame);
            return null; // 返回值可以用于传递处理结果
        }
        

        public void onDrawFrame(android.graphics.Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
            // 在Driver Station屏幕上绘制结果
            // 这里可以留空，因为我们已经在processFrame中在Mat上绘制了结果
        }
        
        public MultiTmpMatcher getMatcher() {
            return matcher;
        }
    }
    
    private static class DetectionResult {
        Rect rect;
        int templateIdx;
        String templateName;
        
        DetectionResult(Rect rect, int templateIdx, String templateName) {
            this.rect = rect;
            this.templateIdx = templateIdx;
            this.templateName = templateName;
        }
    }
    
    public static class TrackedObject {
        private Rect rect;
        private int id;
        private int missingFrames;
        private int templateIdx;
        private String templateName;
        
        public TrackedObject(Rect rect, int id, int templateIdx, String templateName) {
            this.rect = rect;
            this.id = id;
            this.missingFrames = 0;
            this.templateIdx = templateIdx;
            this.templateName = templateName;
        }
        
        public void update(Rect newRect, int templateIdx, String templateName) {
            this.rect = newRect;
            this.missingFrames = 0;
            this.templateIdx = templateIdx;
            this.templateName = templateName;
        }
        
        public void predict() {
        }
        
        public void incrementMissingFrames() {
            missingFrames++;
        }
        
        public Rect getRect() {
            return rect;
        }
        
        public int getId() {
            return id;
        }
        
        public int getMissingFrames() {
            return missingFrames;
        }
        
        public int getTemplateIdx() {
            return templateIdx;
        }
        
        public String getTemplateName() {
            return templateName;
        }
    }
    
    private static class Match {
        int trackerIdx;
        int detectionIdx;
        double iou;
        
        Match(int trackerIdx, int detectionIdx, double iou) {
            this.trackerIdx = trackerIdx;
            this.detectionIdx = detectionIdx;
            this.iou = iou;
        }
    }
}

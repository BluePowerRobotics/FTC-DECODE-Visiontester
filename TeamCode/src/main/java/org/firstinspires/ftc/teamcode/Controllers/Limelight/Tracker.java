package org.firstinspires.ftc.teamcode.Controllers.Limelight;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.Vision.projection.Projector;

import java.util.*;

public class Tracker {
    private Detector detector;
    private Projector projector;
    private List<Target> targets;
    private int nextTargetId;
    private Map<Detection, Integer> detectionHistory;
    private Map<Target, Integer> removalPending;
    private double distanceThreshold;
    private int confirmationFrames;
    private int removalFrames;
    
    /**
     * 构造函数
     * @param hardwareMap 硬件映射
     * @param distanceThreshold 两小球归为同一组的最大距离
     * @param confirmationFrames 新目标需要连续出现的帧数
     * @param removalFrames 目标需要连续消失的帧数
     * @param projector Projector实例
     */
    public Tracker(HardwareMap hardwareMap, double distanceThreshold, int confirmationFrames, int removalFrames, Projector projector) {
        this.detector = new Detector(hardwareMap);
        this.projector = projector;
        this.targets = new ArrayList<>();
        this.nextTargetId = 0;
        this.detectionHistory = new HashMap<>();
        this.removalPending = new HashMap<>();
        this.distanceThreshold = distanceThreshold;
        this.confirmationFrames = confirmationFrames;
        this.removalFrames = removalFrames;
    }
    
    /**
     * 启动Tracker
     */
    public void start() {
        detector.start();
    }
    
    /**
     * 更新所有Target对象的状态
     */
    public void update() {
        // 获取当前帧的检测结果
        List<Detection> currentDetections = getCurrentDetections();
        
        // 对检测框进行空间聚类
        List<List<Detection>> currentGroups = clusterDetections(currentDetections);
        
        // 匹配与更新目标
        matchAndUpdateTargets(currentGroups);
        
        // 处理新目标确认
        handleNewCandidates(currentDetections);
        
        // 处理旧目标移除
        handleMissingTargets();
    }
    
    /**
     * 获取当前帧的检测结果
     * @return 检测结果列表
     */
    private List<Detection> getCurrentDetections() {
        List<Detection> detections = new ArrayList<>();
        
        // 检测purple小球
        double[][] purpleOffsets = detector.get_center("purple");
        for (double[] offset : purpleOffsets) {
            double[] worldPos = projector.project(offset[0], offset[1]);
            detections.add(new Detection("purple", worldPos[0], worldPos[1]));
        }
        
        // 检测green小球
        double[][] greenOffsets = detector.get_center("green");
        for (double[] offset : greenOffsets) {
            double[] worldPos = projector.project(offset[0], offset[1]);
            detections.add(new Detection("green", worldPos[0], worldPos[1]));
        }
        
        return detections;
    }
    
    /**
     * 对检测框进行空间聚类
     * @param detections 检测框列表
     * @return 聚类后的检测框组列表
     */
    private List<List<Detection>> clusterDetections(List<Detection> detections) {
        List<List<Detection>> groups = new ArrayList<>();
        Set<Detection> processed = new HashSet<>();
        
        for (Detection detection : detections) {
            if (processed.contains(detection)) {
                continue;
            }
            
            List<Detection> group = new ArrayList<>();
            Queue<Detection> queue = new LinkedList<>();
            
            group.add(detection);
            queue.add(detection);
            processed.add(detection);
            
            while (!queue.isEmpty()) {
                Detection current = queue.poll();
                for (Detection other : detections) {
                    if (!processed.contains(other) && current.distanceTo(other) <= distanceThreshold) {
                        group.add(other);
                        queue.add(other);
                        processed.add(other);
                    }
                }
            }
            
            groups.add(group);
        }
        
        return groups;
    }
    
    /**
     * 匹配与更新目标
     * @param currentGroups 当前帧的聚类组
     */
    private void matchAndUpdateTargets(List<List<Detection>> currentGroups) {
        Set<Target> matchedTargets = new HashSet<>();
        
        for (List<Detection> group : currentGroups) {
            // 计算组的中心
            double centerX = 0, centerY = 0;
            for (Detection detection : group) {
                centerX += detection.x;
                centerY += detection.y;
            }
            centerX /= group.size();
            centerY /= group.size();
            
            // 寻找匹配的目标
            Target matchedTarget = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Target target : targets) {
                double distance = Math.sqrt(Math.pow(target.centerX - centerX, 2) + Math.pow(target.centerY - centerY, 2));
                if (distance <= distanceThreshold && distance < minDistance) {
                    matchedTarget = target;
                    minDistance = distance;
                }
            }
            
            if (matchedTarget != null) {
                // 更新目标
                matchedTarget.updateMembers(group);
                matchedTarget.lastSeenTimestamp = System.currentTimeMillis();
                matchedTarget.stableCount++;
                matchedTargets.add(matchedTarget);
                // 从待移除列表中移除
                removalPending.remove(matchedTarget);
            } else {
                // 处理未匹配的组，作为候选
                for (Detection detection : group) {
                    int count = detectionHistory.getOrDefault(detection, 0) + 1;
                    detectionHistory.put(detection, count);
                }
            }
        }
        
        // 标记未匹配的目标为待移除
        for (Target target : targets) {
            if (!matchedTargets.contains(target)) {
                int count = removalPending.getOrDefault(target, 0) + 1;
                removalPending.put(target, count);
            }
        }
    }
    
    /**
     * 处理新目标确认
     * @param currentDetections 当前帧的检测结果
     */
    private void handleNewCandidates(List<Detection> currentDetections) {
        List<Detection> toRemove = new ArrayList<>();
        
        for (Map.Entry<Detection, Integer> entry : detectionHistory.entrySet()) {
            Detection detection = entry.getKey();
            int count = entry.getValue();
            
            if (count >= confirmationFrames) {
                // 创建新目标
                Target newTarget = new Target(nextTargetId++);
                newTarget.addDetection(detection);
                targets.add(newTarget);
                toRemove.add(detection);
            } else if (!currentDetections.contains(detection)) {
                // 检测消失，重置计数
                toRemove.add(detection);
            }
        }
        
        for (Detection detection : toRemove) {
            detectionHistory.remove(detection);
        }
    }
    
    /**
     * 处理旧目标移除
     */
    private void handleMissingTargets() {
        List<Target> toRemove = new ArrayList<>();
        
        for (Map.Entry<Target, Integer> entry : removalPending.entrySet()) {
            Target target = entry.getKey();
            int count = entry.getValue();
            
            if (count >= removalFrames) {
                toRemove.add(target);
            }
        }
        
        for (Target target : toRemove) {
            removeTarget(target);
        }
    }
    
    /**
     * 从目标列表中移除指定目标
     * @param target 要移除的目标
     */
    private void removeTarget(Target target) {
        targets.remove(target);
        removalPending.remove(target);
    }
    
    /**
     * 添加新目标
     * @param target 要添加的目标
     */
    private void addTarget(Target target) {
        target.id = nextTargetId++;
        targets.add(target);
    }
    
    /**
     * 获取最优目标
     * @return 最优目标
     */
    public Target getBestTarget() {
        if (targets.isEmpty()) {
            return null;
        }
        
        Target bestTarget = null;
        double bestScore = -1;
        
        for (Target target : targets) {
            double score = computeTargetScore(target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }
        
        return bestTarget;
    }
    
    /**
     * 计算目标得分
     * @param target 目标
     * @return 得分
     */
    private double computeTargetScore(Target target) {
        int memberCount = target.memberDetections.size();
        double distance = Math.sqrt(Math.pow(target.centerX, 2) + Math.pow(target.centerY, 2));
        if (distance == 0) {
            distance = 0.1; // 避免除零
        }
        return memberCount / distance;
    }
    
    /**
     * 获取所有目标
     * @return 目标列表
     */
    public List<Target> getTargets() {
        return targets;
    }
    
    /**
     * 停止Tracker
     */
    public void stop() {
        detector.stop();
    }
    
    /**
     * 检测类
     */
    public static class Detection {
        public final String color;
        public final double x;
        public final double y;
        
        /**
         * 构造函数
         * @param color 小球颜色
         * @param x x坐标
         * @param y y坐标
         */
        public Detection(String color, double x, double y) {
            this.color = color;
            this.x = x;
            this.y = y;
        }
        
        /**
         * 计算与另一个检测的距离
         * @param other 另一个检测
         * @return 距离
         */
        public double distanceTo(Detection other) {
            return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Detection that = (Detection) o;
            return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Objects.equals(color, that.color);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(color, x, y);
        }
    }
    
    /**
     * 目标类
     */
    public static class Target {
        public int id;
        public List<Detection> memberDetections;
        public double centerX;
        public double centerY;
        public long lastSeenTimestamp;
        public int stableCount;
        
        /**
         * 构造函数
         * @param id 目标ID
         */
        public Target(int id) {
            this.id = id;
            this.memberDetections = new ArrayList<>();
            this.centerX = 0;
            this.centerY = 0;
            this.lastSeenTimestamp = System.currentTimeMillis();
            this.stableCount = 0;
        }
        
        /**
         * 更新成员列表并重新计算中心
         * @param detections 新的成员检测列表
         */
        public void updateMembers(List<Detection> detections) {
            this.memberDetections = new ArrayList<>(detections);
            updateCenter();
        }
        
        /**
         * 添加一个检测并更新中心
         * @param detection 检测
         */
        public void addDetection(Detection detection) {
            memberDetections.add(detection);
            updateCenter();
        }
        
        /**
         * 移除一个检测并更新中心
         * @param detection 检测
         */
        public void removeDetection(Detection detection) {
            memberDetections.remove(detection);
            updateCenter();
        }
        
        /**
         * 更新中心坐标
         */
        public void updateCenter() {
            if (memberDetections.isEmpty()) {
                centerX = 0;
                centerY = 0;
                return;
            }
            
            double sumX = 0, sumY = 0;
            for (Detection detection : memberDetections) {
                sumX += detection.x;
                sumY += detection.y;
            }
            
            centerX = sumX / memberDetections.size();
            centerY = sumY / memberDetections.size();
        }
        
        /**
         * 判断成员列表是否为空
         * @return 是否为空
         */
        public boolean isEmpty() {
            return memberDetections.isEmpty();
        }
        
        /**
         * 计算目标中心到摄像头的距离
         * @return 距离
         */
        public double getDistanceToCamera() {
            return Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
        }
    }
}
package org.firstinspires.ftc.teamcode.Controllers.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.List;

public class Detector {
    private Limelight3A limelight;
    private static final int PIPELINE_INDEX = 5; // 使用pipeline5
    
    /**
     * 构造函数
     * @param hardwareMap 硬件映射
     */
    public Detector(HardwareMap hardwareMap) {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.pipelineSwitch(PIPELINE_INDEX);
    }
    
    public void start(){
        if (limelight != null) {
            limelight.start();
        }
    }
    /**
     * 获取指定对象的中心坐标
     * @param objectName 对象名称
     * @return 二维数组，每行表示一个对象的归一化中心坐标 [x, y]
     */
    public double[][] get_center(String objectName) {
        LLResult result = limelight.getLatestResult();
        List<double[]> centers = new ArrayList<>();
        
        if (result != null && result.isValid()) {
            // 获取检测器结果
            List<LLResultTypes.DetectorResult> detectorResults = result.getDetectorResults();
            
            for (LLResultTypes.DetectorResult dr : detectorResults) {
                // 检查对象名称是否匹配
                if (dr.getClassName().equals(objectName)) {
                    // 获取像素坐标偏移（相对于画面中心）
                    double txp = dr.getTargetXPixels();
                    double typ = dr.getTargetYPixels();
                    
                    // 转换为相对于画面中心点的偏移，左上角偏移为正
                    // Limelight默认：txp正值表示右侧，typ正值表示下方
                    // 转换后：x正值表示左侧，y正值表示上方
                    double offsetX = -txp;
                    double offsetY = -typ;
                    
                    centers.add(new double[]{offsetX, offsetY});
                }
            }
        }
        
        // 将List转换为二维数组
        double[][] resultArray = new double[centers.size()][2];
        for (int i = 0; i < centers.size(); i++) {
            resultArray[i] = centers.get(i);
        }
        
        return resultArray;
    }
    
    /**
     * 停止Limelight
     */
    public void stop() {
        if (limelight != null) {
            limelight.stop();
        }
    }
}
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
     * 获取指定对象的归一化坐标
     * @param objectName 对象名称
     * @return 二维数组，每行表示一个对象的归一化坐标 [m, n]，m为x方向偏移(左正右负)，n为y方向偏移(上正下负)
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

                    // 归一化到0-1范围（取画面长边为1，假设图像分辨率为640x480）
                    // m对应x方向偏移，n对应y方向偏移
                    double m = -txp / 320.0; // x方向归一化，左侧为正
                    double n = -typ / 320.0; // y方向归一化，上方为正（均取长边320为基准）

                    centers.add(new double[]{m, n});
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
package org.firstinspires.ftc.teamcode.Vision.projection;

import java.util.HashMap;
import java.util.Map;

public class Projector {
    // 常量定义
    //m0需要根据已知值调整
    private static final double m0 = 1.0; // 已知常量
    private static final double rt2 = Math.sqrt(2); // 根号2
    
    // 摄像头phi值映射
    private static final Map<String, Double> CAMERA_PHI_MAP = new HashMap<>();
    static {
        // 初始化摄像头phi值
        CAMERA_PHI_MAP.put("Limelight", 0.0);
    }
    
    // 画面相关参数
    private final double M; // 画面长边长度
    private final double x0; // 画面中心点x坐标
    private final double y0; // 画面中心点y坐标
    private final double h;  // 摄像头高度
    
    /**
     * 构造函数
     * @param M 画面长边长度
     * @param x0 画面中心点x坐标
     * @param y0 画面中心点y坐标
     * @param h 摄像头高度
     */
    public Projector(double M, double x0, double y0, double h) {
        this.M = M;
        this.x0 = x0;
        this.y0 = y0;
        this.h = h;
    }
    
    /**
     * 计算小球在世界坐标系中的位置
     * @param x_ball 小球在画面中的x坐标
     * @param y_ball 小球在画面中的y坐标
     * @return 小球相对机器人的位置 [x, y]
     */
    public double[] project(double x_ball, double y_ball) {
        // 计算横向偏移
        double m = (x_ball - x0) / M;
        
        // 计算纵向偏移
        double n = (y_ball - y0) / M;
        
        // 计算像距
        double d = (rt2 * m0 * h) / (m0 - n);
        
        // 计算小球相对摄像头的位置
        double delta_x = rt2 * d - h;
        double delta_y = m * d / m0;
        
        // 直接使用小球相对摄像头的位置作为世界坐标系位置
        double x = delta_x;
        double y = delta_y;
        
        return new double[]{x, y};
    }
    
    /**
     * 重载方法，保持兼容性
     * @param x_ball 小球在画面中的x坐标
     * @param y_ball 小球在画面中的y坐标
     * @param theta 小车方向与x轴的夹角（弧度，逆时针为正）
     * @return 小球相对机器人的位置 [x, y]
     */
    public double[] project(double x_ball, double y_ball, double theta) {
        // 忽略theta参数
        return project(x_ball, y_ball);
    }
    
    /**
     * 添加或更新摄像头phi值
     * @param cameraName 摄像头名称
     * @param phi 摄像头方向与小车方向的夹角（弧度，逆时针为正）
     */
    public static void addCameraPhi(String cameraName, double phi) {
        CAMERA_PHI_MAP.put(cameraName, phi);
    }
    
    /**
     * 获取摄像头对应的phi值
     * @param cameraName 摄像头名称
     * @return 摄像头方向与小车方向的夹角（弧度，逆时针为正）
     */
    public static double getCameraPhi(String cameraName) {
        return CAMERA_PHI_MAP.getOrDefault(cameraName, 0.0);
    }
}
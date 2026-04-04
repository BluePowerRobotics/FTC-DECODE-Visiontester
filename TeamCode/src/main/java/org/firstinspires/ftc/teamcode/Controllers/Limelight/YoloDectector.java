import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

public class YoloDectector {
    private NetworkTable limelightTable;
    private Telemetry telemetry;
    
    public YoloDectector(Telemetry telemetry) {
        this.telemetry = telemetry;
        // 初始化NetworkTable
        NetworkTableInstance ntInstance = NetworkTableInstance.getDefault();
        limelightTable = ntInstance.getTable("limelight");
    }
    
    /**
     * 检测指定对象并返回其位置和大小
     * @param objectName 要检测的对象名称
     * @return 检测到的对象信息，包含位置和大小
     */
    public DetectionResult detect(String objectName) {
        // 从NetworkTables读取检测结果
        String detections = limelightTable.getString("detections", "");
        
        if (detections.isEmpty()) {
            telemetry.addData("YoloDectector", "No detections found");
            return null;
        }
        
        // 解析检测结果
        String[] detectionArray = detections.split(";\s*");
        
        for (String detection : detectionArray) {
            String[] parts = detection.split(",\s*");
            if (parts.length >= 6) {
                String className = parts[0];
                double confidence = Double.parseDouble(parts[1]);
                int x1 = Integer.parseInt(parts[2]);
                int y1 = Integer.parseInt(parts[3]);
                int x2 = Integer.parseInt(parts[4]);
                int y2 = Integer.parseInt(parts[5]);
                
                // 检查是否是目标对象
                if (className.equals(objectName)) {
                    // 计算对象的中心位置和大小
                    int centerX = (x1 + x2) / 2;
                    int centerY = (y1 + y2) / 2;
                    int width = x2 - x1;
                    int height = y2 - y1;
                    
                    // 创建并返回检测结果
                    DetectionResult result = new DetectionResult(
                            className,
                            confidence,
                            centerX,
                            centerY,
                            width,
                            height,
                            x1,
                            y1,
                            x2,
                            y2
                    );
                    
                    // 输出检测结果到telemetry
                    telemetry.addData("YoloDectector", "Found object: " + objectName);
                    telemetry.addData("Confidence", "%.2f", confidence);
                    telemetry.addData("Center", "(%d, %d)", centerX, centerY);
                    telemetry.addData("Size", "%dx%d", width, height);
                    telemetry.addData("Bounding Box", "(%d, %d) -> (%d, %d)", x1, y1, x2, y2);
                    
                    return result;
                }
            }
        }
        
        // 未找到目标对象
        telemetry.addData("YoloDectector", "Object not found: " + objectName);
        return null;
    }
    
    /**
     * 检测结果类，包含对象的位置和大小信息
     */
    public static class DetectionResult {
        public final String className;
        public final double confidence;
        public final int centerX;
        public final int centerY;
        public final int width;
        public final int height;
        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;
        
        public DetectionResult(String className, double confidence, int centerX, int centerY, 
                              int width, int height, int x1, int y1, int x2, int y2) {
            this.className = className;
            this.confidence = confidence;
            this.centerX = centerX;
            this.centerY = centerY;
            this.width = width;
            this.height = height;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%.2f): center=(%d,%d), size=%dx%d, box=(%d,%d)-(%d,%d)",
                    className, confidence, centerX, centerY, width, height, x1, y1, x2, y2);
        }
    }
}

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.Controllers.Limelight.YoloDectector;

@TeleOp(name = "YoloTester", group = "Tests")
public class YoloTester extends LinearOpMode {
    private YoloDectector detector;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1秒更新一次
    
    @Override
    public void runOpMode() {
        // 初始化YoloDectector
        detector = new YoloDectector(telemetry);
        
        telemetry.addData("Status", "Initialized");
        telemetry.addData("Instructions", "Press play to start detecting GreenBall and PurpleBall");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            long currentTime = System.currentTimeMillis();
            
            // 每秒更新一次检测结果
            if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
                lastUpdateTime = currentTime;
                
                // 检测GreenBall
                YoloDectector.DetectionResult greenBallResult = detector.detect("GreenBall");
                
                // 检测PurpleBall
                YoloDectector.DetectionResult purpleBallResult = detector.detect("PurpleBall");
                
                // 输出状态信息
                telemetry.addData("Status", "Running");
                telemetry.addData("Update Interval", UPDATE_INTERVAL + "ms");
                telemetry.addData("Current Time", currentTime);
                
                // 输出检测结果
                if (greenBallResult != null) {
                    telemetry.addData("GreenBall", greenBallResult.toString());
                } else {
                    telemetry.addData("GreenBall", "Not detected");
                }
                
                if (purpleBallResult != null) {
                    telemetry.addData("PurpleBall", purpleBallResult.toString());
                } else {
                    telemetry.addData("PurpleBall", "Not detected");
                }
                
                telemetry.update();
            }
        }
    }
}

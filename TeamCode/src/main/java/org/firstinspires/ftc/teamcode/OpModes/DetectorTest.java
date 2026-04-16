package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.Controllers.Limelight.Detector;

@TeleOp(name = "DetectorTest", group = "Tests")
public class DetectorTest extends LinearOpMode {
    private Detector detector;
    
    @Override
    public void runOpMode() {
        // 初始化Detector
        detector = new Detector(hardwareMap);
        
        telemetry.addData("Status", "Initialized");
        telemetry.addData("Instructions", "Press play to start detecting purple and green objects");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            // 检测purple对象
            double[][] purpleCenters = detector.get_center("purple");
            
            // 检测green对象
            double[][] greenCenters = detector.get_center("green");
            
            // 输出状态信息
            telemetry.addData("Status", "Running");
            
            // 输出purple对象的中心坐标
            telemetry.addData("Purple Objects", "Count: " + purpleCenters.length);
            for (int i = 0; i < purpleCenters.length; i++) {
                telemetry.addData("Purple " + i, "(%.2f, %.2f)", purpleCenters[i][0], purpleCenters[i][1]);
            }
            
            // 输出green对象的中心坐标
            telemetry.addData("Green Objects", "Count: " + greenCenters.length);
            for (int i = 0; i < greenCenters.length; i++) {
                telemetry.addData("Green " + i, "(%.2f, %.2f)", greenCenters[i][0], greenCenters[i][1]);
            }
            
            telemetry.update();
        }
        
        // 停止Detector
        detector.stop();
    }
}
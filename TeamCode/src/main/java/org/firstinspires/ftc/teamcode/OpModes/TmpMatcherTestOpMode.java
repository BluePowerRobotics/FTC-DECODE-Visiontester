package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.Vision.TM.TmpMatcher;
import org.firstinspires.ftc.teamcode.Vision.TM.MultiTmpMatcher;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;

import java.util.List;

@TeleOp(name = "TmpMatcher Test OpMode", group = "Test")
public class TmpMatcherTestOpMode extends LinearOpMode {
    private static final boolean USE_WEBCAM = true;  // true for webcam, false for phone camera
    private VisionPortal visionPortal;
    private TmpMatcher purpleMatcher;
    private TmpMatcher greenMatcher;
    private MultiTmpMatcher multiMatcher;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing TmpMatcher...");
        telemetry.update();
        
        // 初始化模板列表
        java.util.ArrayList<String> templates = new java.util.ArrayList<>();
        templates.add("purpleball_template_1.jpg");
        templates.add("greenball_template_1.jpg");
        
        // 初始化模板匹配器
        try {
            // 使用实际存在的模板图像
            purpleMatcher = new TmpMatcher(hardwareMap.appContext, "purpleball_template_1.jpg");
            greenMatcher = new TmpMatcher(hardwareMap.appContext, "greenball_template_1.jpg");
            
            // 初始化多模板匹配器
            multiMatcher = new MultiTmpMatcher(hardwareMap.appContext, templates);
            
            telemetry.addData("TmpMatcher", "Created successfully");
            telemetry.addData("Purple Matcher", "Initialized");
            telemetry.addData("Green Matcher", "Initialized");
            telemetry.addData("Multi Matcher", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Error", "Failed to create TmpMatcher: " + e.getMessage());
        }
        
        // 初始化VisionPortal
        try {
            // 创建VisionPortal构建器
            VisionPortal.Builder builder = new VisionPortal.Builder();

            // 设置相机（网络摄像头或内置手机相机）
            if (USE_WEBCAM) {
                builder.setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"));
            } else {
                builder.setCamera(BuiltinCameraDirection.BACK);
            }

            // 启用实时预览
            builder.enableLiveView(true);

            // 设置流格式为MJPEG，使用更少的带宽
            builder.setStreamFormat(VisionPortal.StreamFormat.MJPEG);

            // 添加处理器（选择其中一个）
            // 注意：一次只能添加一个处理器，因为它们会修改同一个图像
            // 这里我们使用多模板匹配器的处理器
            MultiTmpMatcher.MultiTmpMatcherProcessor multiProcessor = new MultiTmpMatcher.MultiTmpMatcherProcessor(hardwareMap.appContext, templates);
            builder.addProcessor(multiProcessor);
            
            // 更新multiMatcher引用，以便获取检测结果
            multiMatcher = multiProcessor.getMatcher();

            // 构建VisionPortal
            visionPortal = builder.build();
            
            telemetry.addData("Camera", "Initialized successfully");
        } catch (Exception e) {
            telemetry.addData("Camera Error", "Failed to initialize camera: " + e.getMessage());
        }
        
        telemetry.addData("Status", "Initialized");
        telemetry.addData("DS preview on/off", "3 dots, Camera Stream");
        telemetry.addData(">", "Touch START to start OpMode");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            // 控制相机流
            if (gamepad1.dpad_down) {
                visionPortal.stopStreaming();
            } else if (gamepad1.dpad_up) {
                visionPortal.resumeStreaming();
            }
            
            // 显示检测结果
            try {
                // 显示多模板匹配结果（因为我们使用的是multiMatcher）
                List<MultiTmpMatcher.TrackedObject> multiObjects = multiMatcher.getTrackedObjects();
                telemetry.addData("Detected Objects", multiObjects.size());
                for (int i = 0; i < multiObjects.size(); i++) {
                    MultiTmpMatcher.TrackedObject obj = multiObjects.get(i);
                    org.opencv.core.Rect rect = obj.getRect();
                    telemetry.addData("Object " + i, "Type: " + obj.getTemplateName() + " ID: " + obj.getId() + " Position: (" + rect.x + ", " + rect.y + ") Size: " + rect.width + "x" + rect.height);
                }
            } catch (Exception e) {
                telemetry.addData("Error", "Failed to get tracked objects: " + e.getMessage());
            }
            
            telemetry.update();
            
            // 共享CPU
            sleep(20);
        }
        
        // 关闭VisionPortal以节省资源
        visionPortal.close();
    }
}

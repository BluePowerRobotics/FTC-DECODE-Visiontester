package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.Vision.TM.TmpMatcher;
import org.firstinspires.ftc.teamcode.Vision.TM.MultiTmpMatcher;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.easyopencv.OpenCvPipeline;
import org.opencv.core.Mat;

import java.util.List;

@TeleOp(name = "TmpMatcher Test OpMode", group = "Test")
public class TmpMatcherTestOpMode extends LinearOpMode {
    private OpenCvCamera camera;
    private TmpMatcher purpleMatcher;
    private TmpMatcher greenMatcher;
    private MultiTmpMatcher multiMatcher;
    private boolean isProcessing = false;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing TmpMatcher...");
        telemetry.update();
        
        // 初始化模板匹配器
        try {
            // 使用实际存在的模板图像
            purpleMatcher = new TmpMatcher("purpleball_template_1.jpg");
            greenMatcher = new TmpMatcher("greenball_template_1.jpg");
            
            // 初始化多模板匹配器
            java.util.ArrayList<String> templates = new java.util.ArrayList<>();
            templates.add("purpleball_template_1.jpg");
            templates.add("greenball_template_1.jpg");
            multiMatcher = new MultiTmpMatcher(templates);
            
            telemetry.addData("TmpMatcher", "Created successfully");
            telemetry.addData("Purple Matcher", "Initialized");
            telemetry.addData("Green Matcher", "Initialized");
            telemetry.addData("Multi Matcher", "Initialized");
        } catch (Exception e) {
            telemetry.addData("Error", "Failed to create TmpMatcher: " + e.getMessage());
        }
        
        // 初始化摄像头
        try {
            int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
            camera = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);

            // 设置相机回调
            camera.setPipeline(new OpenCvPipeline() {
                @Override
                public Mat processFrame(Mat input) {
                    if (isProcessing) {
                        // 处理图像
                        try {
                            purpleMatcher.processFrame(input);
                            greenMatcher.processFrame(input);
                            multiMatcher.processFrame(input);
                        } catch (Exception e) {
                            telemetry.addData("Processing Error", e.getMessage());
                        }
                    }
                    return input;
                }
            });

            // 启动相机
            camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
                @Override
                public void onOpened() {
                    camera.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
                }

                @Override
                public void onError(int errorCode) {
                    telemetry.addData("Camera Error", errorCode);
                    telemetry.update();
                }
            });
            
            telemetry.addData("Camera", "Initialized successfully");
        } catch (Exception e) {
            telemetry.addData("Camera Error", "Failed to initialize camera: " + e.getMessage());
        }
        
        telemetry.addData("Status", "Initialized");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            // 切换处理状态
            if (gamepad1.a) {
                isProcessing = !isProcessing;
                telemetry.addData("Processing", isProcessing);
                telemetry.update();
                sleep(500);
            }
            
            // 显示检测结果
            if (isProcessing) {
                try {
                    // 显示紫色小球检测结果
                    List<TmpMatcher.TrackedObject> purpleObjects = purpleMatcher.getTrackedObjects();
                    telemetry.addData("Purple Balls", purpleObjects.size());
                    for (TmpMatcher.TrackedObject obj : purpleObjects) {
                        org.opencv.core.Rect rect = obj.getRect();
                        telemetry.addData("Purple ID " + obj.getId(), "(" + rect.x + ", " + rect.y + ") " + rect.width + "x" + rect.height);
                    }

                    // 显示绿色小球检测结果
                    List<TmpMatcher.TrackedObject> greenObjects = greenMatcher.getTrackedObjects();
                    telemetry.addData("Green Balls", greenObjects.size());
                    for (TmpMatcher.TrackedObject obj : greenObjects) {
                        org.opencv.core.Rect rect = obj.getRect();
                        telemetry.addData("Green ID " + obj.getId(), "(" + rect.x + ", " + rect.y + ") " + rect.width + "x" + rect.height);
                    }

                    // 显示多模板匹配结果
                    List<MultiTmpMatcher.TrackedObject> multiObjects = multiMatcher.getTrackedObjects();
                    telemetry.addData("Multi Objects", multiObjects.size());
                    for (MultiTmpMatcher.TrackedObject obj : multiObjects) {
                        org.opencv.core.Rect rect = obj.getRect();
                        telemetry.addData("Multi ID " + obj.getId(), "Type: " + obj.getTemplateName() + " Position: (" + rect.x + ", " + rect.y + ") Size: " + rect.width + "x" + rect.height);
                    }
                } catch (Exception e) {
                    telemetry.addData("Error", "Failed to get tracked objects: " + e.getMessage());
                }
            } else {
                telemetry.addData("Status", "Press A to start processing");
            }
            
            telemetry.update();
        }
        
        // 停止相机
        try {
            camera.stopStreaming();
            camera.closeCameraDevice();
        } catch (Exception e) {
            telemetry.addData("Error", "Failed to close camera: " + e.getMessage());
        }
    }
}

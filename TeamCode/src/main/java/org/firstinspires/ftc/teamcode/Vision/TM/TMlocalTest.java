package org.firstinspires.ftc.teamcode.Vision.TM;

import android.content.Context;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionPortal;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

@TeleOp(name = "TMlocalTest", group = "Vision")
public class TMlocalTest extends LinearOpMode {
    private VisionPortal visionPortal;
    private MultiTmpMatcher.MultiTmpMatcherProcessor multiProcessor;
    private TmpMatcher.TmpMatcherProcessor singleProcessor;
    private boolean useMultiMatcher = true; // 切换使用哪个匹配器

    @Override
    public void runOpMode() {
        // 初始化匹配器
        Context context = hardwareMap.appContext;
        
        // 使用现有的小球模板图像
        List<String> ballTemplates = new ArrayList<>();
        ballTemplates.add("greenball_template_1");
        ballTemplates.add("greenball_template_2");
        ballTemplates.add("greenball_template_3");
        ballTemplates.add("purpleball_template_1");
        ballTemplates.add("purpleball_template_2");
        ballTemplates.add("purpleball_template_3");
        
        // 初始化多模板匹配器（使用多个模板）
        multiProcessor = new MultiTmpMatcher.MultiTmpMatcherProcessor(context, ballTemplates);
        
        // 初始化单模板匹配器（使用第一个模板）
        singleProcessor = new TmpMatcher.TmpMatcherProcessor(context, "greenball_template_1");
        
        // 创建VisionPortal
        VisionPortal.Builder builder = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(com.qualcomm.robotcore.hardware.WebcamName.class, "PC Camera"))
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG);
        
        // 添加处理器
        if (useMultiMatcher) {
            builder.addProcessor(multiProcessor);
        } else {
            builder.addProcessor(singleProcessor);
        }
        
        visionPortal = builder.build();
        
        telemetry.addData("Status", "Initialized");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            // 获取识别结果
            List<?> trackedObjects;
            if (useMultiMatcher) {
                trackedObjects = multiProcessor.getMatcher().getTrackedObjects();
            } else {
                trackedObjects = singleProcessor.getMatcher().getTrackedObjects();
            }
            
            // 输出识别到的位置
            telemetry.addData("Tracked Objects", trackedObjects.size());
            
            for (int i = 0; i < trackedObjects.size(); i++) {
                Object obj = trackedObjects.get(i);
                Rect rect;
                String type = "Unknown";
                
                if (obj instanceof MultiTmpMatcher.TrackedObject) {
                    MultiTmpMatcher.TrackedObject trackedObj = (MultiTmpMatcher.TrackedObject) obj;
                    rect = trackedObj.getRect();
                    type = trackedObj.getTemplateName();
                } else if (obj instanceof TmpMatcher.TrackedObject) {
                    TmpMatcher.TrackedObject trackedObj = (TmpMatcher.TrackedObject) obj;
                    rect = trackedObj.getRect();
                } else {
                    continue;
                }
                
                // 计算中心点
                int centerX = rect.x + rect.width / 2;
                int centerY = rect.y + rect.height / 2;
                
                telemetry.addData("Object " + i, "Type: " + type + ", Position: (" + centerX + ", " + centerY + "), Size: " + rect.width + "x" + rect.height);
            }
            
            telemetry.update();
            
            // 短暂休眠，避免占用过多资源
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // 关闭VisionPortal
        if (visionPortal != null) {
            visionPortal.close();
        }
    }
}

package org.firstinspires.ftc.teamcode.OpModes;

import android.util.Size;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.teamcode.Vision.TM.MultiTmpMatcherProcessor;
import org.firstinspires.ftc.teamcode.Vision.TM.MultiTmpMatcher;

import java.util.List;

@TeleOp(name = "TmpMatcher Test OpMode", group = "Test")
public class TmpMatcherTestOpMode extends LinearOpMode {
    private static final boolean USE_WEBCAM = true;  // true for webcam, false for phone camera

    private VisionPortal visionPortal;
    private MultiTmpMatcherProcessor multiTmpMatcherProcessor;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initializing TmpMatcher...");
        telemetry.update();
        
        initTmpMatcher();

        // Wait for the DS start button to be touched.
        telemetry.addData("DS preview on/off", "3 dots, Camera Stream");
        telemetry.addData(">", "Touch START to start OpMode");
        telemetry.update();
        waitForStart();

        if (opModeIsActive()) {
            while (opModeIsActive()) {
                telemetryTmpMatcher();

                // Push telemetry to the Driver Station.
                telemetry.update();

                // Save CPU resources; can resume streaming when needed.
                if (gamepad1.dpad_down) {
                    visionPortal.stopStreaming();
                } else if (gamepad1.dpad_up) {
                    visionPortal.resumeStreaming();
                }

                // Share the CPU.
                sleep(20);
            }
        }

        // Save more CPU resources when camera is no longer needed.
        visionPortal.close();
    }

    /**
     * Initialize the TmpMatcher processor.
     */
    private void initTmpMatcher() {
        // Create the MultiTmpMatcher processor.
        java.util.ArrayList<String> templates = new java.util.ArrayList<>();
        templates.add("purpleball_template_1.jpg");
        templates.add("greenball_template_1.jpg");
        multiTmpMatcherProcessor = new MultiTmpMatcherProcessor(templates);

        // Create the vision portal by using a builder.
        VisionPortal.Builder builder = new VisionPortal.Builder();

        // Set the camera (webcam vs. built-in RC phone camera).
        if (USE_WEBCAM) {
            builder.setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"));
        } else {
            builder.setCamera(BuiltinCameraDirection.BACK);
        }

        // Choose a camera resolution. Not all cameras support all resolutions.
        builder.setCameraResolution(new Size(640, 480));

        // Enable the RC preview (LiveView).  Set "false" to omit camera monitoring.
        builder.enableLiveView(true);

        // Set the stream format; MJPEG uses less bandwidth than default YUY2.
        //builder.setStreamFormat(VisionPortal.StreamFormat.YUY2);

        // Choose whether or not LiveView stops if no processors are enabled.
        // If set "true", monitor shows solid orange screen if no processors enabled.
        // If set "false", monitor shows camera view without annotations.
        //builder.setAutoStopLiveView(false);

        // Set and enable the processor.
        builder.addProcessor(multiTmpMatcherProcessor);

        // Build the Vision Portal, using the above settings.
        visionPortal = builder.build();

        // Disable or re-enable the processor at any time.
        //visionPortal.setProcessorEnabled(multiTmpMatcherProcessor, true);
    }

    /**
     * Add telemetry about TmpMatcher detections.
     */
    private void telemetryTmpMatcher() {
        List<MultiTmpMatcher.TrackedObject> currentDetections = multiTmpMatcherProcessor.getMultiTmpMatcher().getTrackedObjects();
        telemetry.addData("# Objects Detected", currentDetections.size());

        // Step through the list of detections and display info for each one.
        for (MultiTmpMatcher.TrackedObject detection : currentDetections) {
            org.opencv.core.Rect rect = detection.getRect();
            telemetry.addLine(String.format("\n==== (ID %d) %s", detection.getId(), detection.getTemplateName()));
            telemetry.addLine(String.format("Position: (%d, %d) Size: %dx%d", rect.x, rect.y, rect.width, rect.height));
        }
    }
}

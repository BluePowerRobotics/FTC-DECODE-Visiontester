package org.firstinspires.ftc.teamcode.rubbishbin;


import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

@TeleOp
public class Limelight extends LinearOpMode {
    private Limelight3A limelight;
//    public Telemetry telemetry;
    public static double tx;
    public static double ty;
    public static double txnc;
    public static double tync;


    @Override
    public void runOpMode() throws InterruptedException
    {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");

        telemetry.setMsTransmissionInterval(11);

        limelight.pipelineSwitch(0);

        /*
         * Starts polling for data.  If you neglect to call start(), getLatestResult() will return null.
         */
        limelight.start();
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            if (gamepad2.yWasPressed()){
                LLStatus status = limelight.getStatus();
                telemetry.addData("Name", "%s",
                        status.getName());
                telemetry.addData("LL", "Temp: %.1fC, CPU: %.1f%%, FPS: %d",
                        status.getTemp(), status.getCpu(),(int)status.getFps());
                telemetry.addData("Pipeline", "Index: %d, Type: %s",
                        status.getPipelineIndex(), status.getPipelineType());
                if (limelight.getLatestResult() != null) {
                    LLResult result = limelight.getLatestResult();

                    if (result.isValid()) {
                        // Access general information
                        Pose3D botpose = result.getBotpose();
                        double captureLatency = result.getCaptureLatency();
                        double targetingLatency = result.getTargetingLatency();
                        double parseLatency = result.getParseLatency();
                        //                telemetry.addData("LL Latency", captureLatency + targetingLatency);
                        //                telemetry.addData("Parse Latency", parseLatency);
                        //                telemetry.addData("PythonOutput", java.util.Arrays.toString(result.getPythonOutput()));
                        tx = result.getTx();
                        ty = result.getTy();
                        txnc = result.getTxNC();
                        tync = result.getTyNC();
                        //                    telemetry.addData("tx", tx);
                        //                    telemetry.addData("txnc", txnc);
                        //                    telemetry.addData("ty", ty);
                        //                    telemetry.addData("tync", tync);

                        //                telemetry.addData("Botpose", botpose.toString());
                        //                    telemetry.update();

                    }

                    //                // Access fiducial results
                    //                List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();
                    //                for (LLResultTypes.FiducialResult fr : fiducialResults) {
                    //                    telemetry.addData("Fiducial", "ID: %d, Family: %s, X: %.2f, Y: %.2f", fr.getFiducialId(), fr.getFamily(), fr.getTargetXDegrees(), fr.getTargetYDegrees());
                    //                }
                }

            }


//            telemetry.update();
        }
        limelight.stop();
    }

}

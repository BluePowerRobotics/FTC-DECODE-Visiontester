package org.firstinspires.ftc.teamcode.Controllers.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

public class LLresult {
    private Limelight3A limelight;
    public Telemetry telemetry;
    public static double tx;
    public static double ty;
    public static double txnc;
    public static double tync;

    public LLresult(HardwareMap hardwareMap){
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        telemetry.setMsTransmissionInterval(11);

        limelight.pipelineSwitch(0);

        /*
         * Starts polling for data.  If you neglect to call start(), getLatestResult() will return null.
         */
        limelight.start();
        telemetry.update();
    }

    public void update(){
        LLResult result = limelight.getLatestResult();
        if (result.isValid()) {
            // Access general information
            Pose3D botpose = result.getBotpose();
            double captureLatency = result.getCaptureLatency();
            double targetingLatency = result.getTargetingLatency();
            double parseLatency = result.getParseLatency();
            tx = result.getTx();
            ty = result.getTy();
            txnc = result.getTxNC();
            tync = result.getTyNC();
        }
    }
    public double getTx(){
        return  tx;
    }
    public double getTy(){
        return  ty;
    }
    public double getTxnc(){
        return  txnc;
    }
    public double getTync(){
        return  tync;
    }



}

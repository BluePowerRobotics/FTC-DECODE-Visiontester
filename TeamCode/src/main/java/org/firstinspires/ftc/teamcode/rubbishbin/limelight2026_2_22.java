package org.firstinspires.ftc.teamcode.rubbishbin;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.List;

@Config
@TeleOp(name = "Senser: Limelight3A",group = "Sensor")

public class limelight2026_2_22 extends LinearOpMode {
    private Limelight3A limelight3A;
    public double TargetXDegreesmax = 10;
    public double TargetXDegreesmin = -10;
    public double TargetSpeed = 30;
    public double StopSpeed = 0;
    public double TargetAngle = 0;
    Motor_PID Motor_PID = new Motor_PID();

//    public double TargetSpeed_FORWARD = 50;
//    public double TargetSpeed_REVERSE = -50;

    @Override
    public void runOpMode() throws InterruptedException {
        limelight3A = hardwareMap.get(Limelight3A.class, "limelight");
        Motor_PID.motor(hardwareMap,telemetry,"motor",false);

        telemetry.setMsTransmissionInterval(11);

        limelight3A.pipelineSwitch(0);

        limelight3A.start();
        telemetry.addData(">","Robot Ready.  Press Play.");
        waitForStart();

        while (opModeIsActive()){
            LLStatus status = limelight3A.getStatus();

            telemetry.addData("Name", "%s", status.getName());
            telemetry.addData("LL", "Temp: %.1fC, CPU: %.1f%%, FPS: %d", status.getTemp(), status.getCpu(), (int) status.getFps());
            telemetry.addData("Pipeline", "Index: %d, Type: %s", status.getPipelineIndex(), status.getPipelineType());

            LLResult result = limelight3A.getLatestResult();
            if (result.isValid()) {
                // Access general information
                Pose3D botpose = result.getBotpose();
                double captureLatency = result.getCaptureLatency();
                double targetingLatency = result.getTargetingLatency();
                double parseLatency = result.getParseLatency();
                telemetry.addData("LL Latency", captureLatency + targetingLatency);
                telemetry.addData("Parse Latency", parseLatency);


                telemetry.addData("tx", result.getTx());
                telemetry.addData("txnc", result.getTxNC());
                telemetry.addData("ty", result.getTy());
                telemetry.addData("tync", result.getTyNC());

//                telemetry.addData("Botpose", botpose.toString());

                List<LLResultTypes.FiducialResult> fiducialResults = result.getFiducialResults();

                for (LLResultTypes.FiducialResult fr : fiducialResults) {

                    telemetry.addData("Fiducial", "ID: %d, Family: %s, X: %.2f, Y: %.2f",
                            fr.getFiducialId(), fr.getFamily(), fr.getTargetXDegrees(), fr.getTargetYDegrees());

                    if (result.getTx() > TargetXDegreesmax) {
                        Motor_PID.turn(TargetSpeed, TargetAngle, result.getTx());
                        telemetry.addData("TurnAction: ","Left_Turning");
                        telemetry.addData("TurnSpeed: ",Motor_PID.getVelocity());
                    }
                    else if (result.getTx() < TargetXDegreesmin){
                        
                        Motor_PID.turn(-TargetSpeed, TargetAngle, result.getTx());
                        telemetry.addData("TurnAction: ","Right_Turning");
                        telemetry.addData("TurnSpeed: ",Motor_PID.getVelocity());
                    }
                    else {
                        Motor_PID.block();
                        telemetry.addData("TurnAction: ","Stop");
                    }
//                    telemetry.update();
                }
                telemetry.update();
            }
            limelight3A.stop();
        }

    }
}

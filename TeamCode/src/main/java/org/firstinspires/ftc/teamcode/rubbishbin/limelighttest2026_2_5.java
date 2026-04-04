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
@TeleOp(name = "Sensor: Limelight3A", group = "Sensor")

public class limelighttest2026_2_5 extends LinearOpMode {


    private Limelight3A limelight;
//    public DcMotor motor;
    public double TargetXDegreesmax = 10;
    public double TargetXDegreesmin = -10;

//    public Servo servo;


    @Override
    public void runOpMode() throws InterruptedException {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");

        telemetry.setMsTransmissionInterval(11);

        limelight.pipelineSwitch(0);
        motor1 motor1 = new motor1();

//        servo = hardwareMap.get(Servo.class, "servo");
        /*
         * Starts polling for data.  If you neglect to call start(), getLatestResult() will return null.
         */
        limelight.start();

        telemetry.addData(">", "Robot Ready.  Press Play.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            LLStatus status = limelight.getStatus();
            telemetry.addData("Name", "%s", status.getName());
            telemetry.addData("LL", "Temp: %.1fC, CPU: %.1f%%, FPS: %d", status.getTemp(), status.getCpu(), (int) status.getFps());
            telemetry.addData("Pipeline", "Index: %d, Type: %s", status.getPipelineIndex(), status.getPipelineType());

            LLResult result = limelight.getLatestResult();
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
                    telemetry.addData("Fiducial", "ID: %d, Family: %s, X: %.2f, Y: %.2f", fr.getFiducialId(), fr.getFamily(), fr.getTargetXDegrees(), fr.getTargetYDegrees());
                    if (result.getTx() > TargetXDegreesmax) {
                        motor1.Motor(hardwareMap, telemetry, "motor", false);
                        motor1.motor_1();
                        motor1.motortelemetry();

                    } else if (result.getTx() < TargetXDegreesmin) {
                        motor1.Motor(hardwareMap, telemetry, "motor", true);
                        motor1.motor_1();
                        motor1.motortelemetry();

                    } else {
                        motor1.motor_3();
                        motor1.motortelemetry();

                    }
                    telemetry.update();
                }
                //OR
//                for (LLResultTypes.FiducialResult fr : fiducialResults) {
//                    telemetry.addData("Fiducial", "ID: %d, Family: %s, X: %.2f, Y: %.2f", fr.getFiducialId(), fr.getFamily(), fr.getTargetXDegrees(), fr.getTargetYDegrees());
//                    if (fr.getTargetXDegrees()>0 || fr.getTargetXDegrees()<0) {
//                        servo.setPosition(result.getTx() / 360.0);
//                    }
//                    else {
//                        return;
//                    }
//                 }

                telemetry.update();
            }
            limelight.stop();
        }
    }
}



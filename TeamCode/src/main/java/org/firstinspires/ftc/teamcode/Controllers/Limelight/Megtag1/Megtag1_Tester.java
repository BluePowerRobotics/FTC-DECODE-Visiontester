package org.firstinspires.ftc.teamcode.Controllers.Limelight.Megtag1;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;


import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.Controllers.Limelight.Limelight_new;

import java.util.List;

@Config
@TeleOp(name = "Megtag1_Tester", group = "Tests")
public class Megtag1_Tester extends LinearOpMode {
    private Limelight3A limelight;
//    private Telemetry telemetry;
    public DcMotorEx motor;

    LLResult result = limelight.getLatestResult();
    LLStatus status = limelight.getStatus();
    public enum ROBOT_STATUS {
        TURNING,
        WAITING,
        STOP;
    }

    ROBOT_STATUS robotStatus = ROBOT_STATUS.WAITING;

    public enum TEAM_COLOR {
        RED, BLUE
    }

    TEAM_COLOR teamColor;
    void Init() {

        //todo set team color
        if (gamepad1.bWasPressed()){
            teamColor = TEAM_COLOR.BLUE;
        } else if (gamepad1.aWasPressed()) {
            teamColor = TEAM_COLOR.RED;
        }


        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
//        telemetry = InstanceTelemetry.init(telemetry);

    }
    @Override
    public void runOpMode() throws InterruptedException {
        //初始化
        Init();
        limelight = hardwareMap.get(Limelight3A.class, "limelight");

        motor = hardwareMap.get(DcMotorEx.class, "motor");
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setDirection(DcMotorSimple.Direction.FORWARD);

        telemetry.setMsTransmissionInterval(11);

        if (teamColor == TEAM_COLOR.BLUE) {
            limelight.pipelineSwitch(0);
        }else if (teamColor == TEAM_COLOR.RED){
            limelight.pipelineSwitch(1);
        }

        telemetry.update();

        telemetry.addData(">", "Robot Ready.  Press Play.");
        waitForStart();

        while (opModeIsActive()) {
            telemetry.addData("Name", "%s",
                    status.getName());
            telemetry.addData("LL", "Temp: %.1fC, CPU: %.1f%%, FPS: %d",
                    status.getTemp(), status.getCpu(),(int)status.getFps());
            telemetry.addData("Pipeline", "Index: %d, Type: %s",
                    status.getPipelineIndex(), status.getPipelineType());

            if (result != null && result.isValid()) {
                if (gamepad1.yWasPressed()) {
                    Pose3D botpose = result.getBotpose();
                    double captureLatency = result.getCaptureLatency();
                    double targetingLatency = result.getTargetingLatency();
                    double parseLatency = result.getParseLatency();
                    telemetry.addData("Botpose", botpose.toString());
                    telemetry.addData("LL Latency", captureLatency + targetingLatency);

                    List<LLResultTypes.BarcodeResult> barcodeResults = result.getBarcodeResults();
                    for (LLResultTypes.BarcodeResult br : barcodeResults) {
                        telemetry.addData("Barcode", "Data: %s", br.getData());
                    }
                }

            }
        }

    }
}

package org.firstinspires.ftc.teamcode.Controllers.Limelight;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.Telemetry;

@Config
@TeleOp(name = "Limelight_new",group = "Tests")
public class Limelight_new extends LinearOpMode {
    private Limelight3A limelight;
    public DcMotorEx motor;

    public double forwardspeed = 30;
    public double reversespeed = -30;
    public double StopSpeed = 0;

    public double maxtx = 1;
    public double mintx = -1;

    public  boolean isturning = false;
    public  boolean hasTarget = false;

//    public Telemetry telemetry;
//    public double TargetXDegreesmax = 10;
//    public double TargetXDegreesmin = -10;
    public double TargetAngle = 0;
    public double TargetSpeed = 30;
//    public double tx;
//    public double ty;
//    public double txnc;
//    public double tync;
//    Motor_PID Motor_PID = new Motor_PID();
//    LLStatus status = limelight.getStatus();
//    TurnTester_PID turnTesterPid;

    boolean turning(double maxtx, double mintx) {
        LLResult result = limelight.getLatestResult();
        if (result != null && result.isValid()){

            if (!hasTarget) {
                isturning = true;
                telemetry.addData("IsTurning", "true");
                if (result.getTx() > maxtx) {
//                    Motor_PID.turn(TargetSpeed, TargetAngle, result.getTx());
                    motor.setPower(forwardspeed);
                    telemetry.addData("TurnAction: ","Left_Turning");
                    telemetry.addData("TurnSpeed: ",forwardspeed);
                    telemetry.addData("tx",result.getTx());
                    hasTarget = false;
                    return false;
                }
                else if (result.getTx() < mintx){
                    motor.setPower(reversespeed);
//                    Motor_PID.turn(-TargetSpeed, TargetAngle, result.getTx());
                    telemetry.addData("TurnAction: ","Right_Turning");
                    telemetry.addData("TurnSpeed: ",reversespeed);
                    telemetry.addData("tx",result.getTx());
                    hasTarget = false;
                    return false;
                }
                else {
//                    Motor_PID.block();
                    motor.setPower(StopSpeed);
                    telemetry.addData("TurnAction: ","Stop");
                    telemetry.addData("tx",result.getTx());
                    isturning = false;
                    hasTarget = true;
                    return false;

                }
            }

        }else {
            telemetry.addData("Limelight", "No data available");
            return true;
        }
        return true;
    }



    @Override
    public void runOpMode() throws InterruptedException {
        //初始化
//        Motor_PID.motor(hardwareMap,telemetry,"motor",false);
        motor = hardwareMap.get(DcMotorEx.class, "motor");
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setDirection(DcMotorSimple.Direction.FORWARD);
        limelight = hardwareMap.get(Limelight3A.class, "limelight");

        telemetry.setMsTransmissionInterval(11);

        limelight.pipelineSwitch(0);


        telemetry.update();


        telemetry.addData(">", "Robot Ready.  Press Play.");
        waitForStart();
        while (opModeIsActive()){
//            Motor_PID.motor(hardwareMap,telemetry,"motor",false);
            LLStatus status = limelight.getStatus();
            LLResult result = limelight.getLatestResult();
            telemetry.addData("Name", "%s",
                    status.getName());
            telemetry.addData("LL", "Temp: %.1fC, CPU: %.1f%%, FPS: %d",
                    status.getTemp(), status.getCpu(),(int)status.getFps());
            telemetry.addData("Pipeline", "Index: %d, Type: %s",
                    status.getPipelineIndex(), status.getPipelineType());
            if (result != null && result.isValid()) {
                if (result.getTx() > maxtx || result.getTx() < mintx ){
                    hasTarget = false;
                }else {
                    hasTarget = true;
                }
            } else {
                hasTarget = false;
            }
            if (gamepad1.yWasPressed()){
                limelight.start();
                while (!hasTarget && opModeIsActive()) {
                    turning(maxtx, mintx);
                    telemetry.update();
                }
            } else if (gamepad1.xWasPressed()) {
                limelight.stop();
            }
            telemetry.update();
        }
    }

}

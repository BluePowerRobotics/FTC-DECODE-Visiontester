package org.firstinspires.ftc.teamcode.rubbishbin;


import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.Controllers.Limelight.LLresult;


@Config
public class Limelight_Calculater {
    Telemetry telemetry;
    LLresult llresult;


    public static double tx = Limelight.tx;
    public static double ty = Limelight.ty;
    public static double txnc = Limelight.txnc;
    public static double tync = Limelight.tync;
    public static boolean hasTarget = false;
    public static boolean isturning = false;
    TurnTester_PID turnTester_pid;
    public Limelight_Calculater(HardwareMap hardwareMap,Telemetry telemetry){
        turnTester_pid = new TurnTester_PID(hardwareMap,telemetry);
        this.telemetry = telemetry;
    }


    public boolean turning(boolean isActive) {

         if (isActive){
             tx = Limelight.tx;
             ty = Limelight.ty;
             txnc = Limelight.txnc;
             tync = Limelight.tync;

         }
        if (!hasTarget){
            isturning = true;
            telemetry.addData("IsTurning","true");

            if (tx > 0) {
                turnTester_pid.targeting(-1);
                telemetry.addData("TurnDirection", "Right");
                telemetry.addData("tx",tx);
            } else if (tx < 0) {
                turnTester_pid.targeting(1);
                telemetry.addData("TurnDirection", "Left");
                telemetry.addData("tx",tx);
            }
            telemetry.addData("tx",tx);
            telemetry.update();
            return isturning;
        }else {
            isturning = false;
            turnTester_pid.targeting(0);
            telemetry.addData("IsTurning","false");
            telemetry.addData("tx",tx);
            telemetry.update();
            return isturning;
        }
    }

}

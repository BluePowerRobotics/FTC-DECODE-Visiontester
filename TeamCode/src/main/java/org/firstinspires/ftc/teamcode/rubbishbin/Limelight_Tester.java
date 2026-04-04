package org.firstinspires.ftc.teamcode.rubbishbin;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
public class Limelight_Tester extends LinearOpMode {
    Limelight_Calculater limelight_calculater;
    TurnTester_PID turnTesterPid;


    @Override
    public void runOpMode() throws InterruptedException {
        limelight_calculater = new Limelight_Calculater(hardwareMap,telemetry);
        waitForStart();
        while (opModeIsActive()) {
            if (gamepad2.yWasPressed()) {
                limelight_calculater.turning(true);
            }
        }
    }


}

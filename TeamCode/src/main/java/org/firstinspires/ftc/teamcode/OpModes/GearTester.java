package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Controllers.Turret.Shooter.ShooterAction;
import org.firstinspires.ftc.teamcode.Controllers.Turret.turner.TurretAimController;
import org.firstinspires.ftc.teamcode.Controllers.Turret.turner.TurretBoardController;

@TeleOp(name = "GearTester", group = "Tests")
public class GearTester extends LinearOpMode {
    TurretBoardController board;
    ShooterAction shooter;
    HardwareMap hardwareMap;
    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        // 初始化双飞轮系统，根据实际电机名称和反转设置调整
        shooter = new ShooterAction(hardwareMap, telemetry, "left_shooter", "right_shooter", true, false);
        board = new TurretBoardController(hardwareMap, "servo");

        waitForStart();

    }

}

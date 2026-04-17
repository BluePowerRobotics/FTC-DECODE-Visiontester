package org.firstinspires.ftc.teamcode.Vision;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.hardware.limelightvision.LLResult;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;


import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import com.qualcomm.robotcore.hardware.IMU;

/*
 * 本文件是一个 线性的（Linear) "OpMode" ，FTC SDK支持两种模式：
 * 一种是 Linear 模式，参考 BasicOpMode_Linear.java。这种模式代码比较直白。
 * 另一种是 Iterative 模式，参考 BasicOpMode_Iterative.java，这种模式有点像 Arduino 的模式
 * OpMode 指的是一段可以自动或者手动操控的程序，在 Driver Station 的程序列表里会显示名字，可以选择并执行对应代码。
 *
 * 这份文件里包含的是一个操控4个麦克纳姆轮底盘的程序。可以参考：
 * https://gm0.org/en/latest/docs/robot-design/drivetrains/holonomic.html
 * 注意，麦克纳姆轮正确安装时，从上往下看起来应该像 X
 */
// 下方的 TeleOp 表示时手动操控阶段，可以改成 Autonomous ，即为自动阶段代码。// name 部份为DS上显示的名字，group用于分组
@TeleOp(name="limelighttest1", group="Tests")
public class limelighttest extends LinearOpMode {

    // 声明四个电机变量
    private DcMotor leftFrontDrive = null;
    private DcMotor leftBackDrive = null;
    private DcMotor rightFrontDrive = null;
    private DcMotor rightBackDrive = null;
    private IMU imu;
    private Limelight3A limelight;

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());




        // 初始化硬件，注意下面的字符串必须跟在DS或者RC上 configure 里配置的名字完全一致。
        leftFrontDrive  = hardwareMap.get(DcMotor.class, "fL");
        leftBackDrive  = hardwareMap.get(DcMotor.class, "bL");
        rightFrontDrive = hardwareMap.get(DcMotor.class, "fR");
        rightBackDrive = hardwareMap.get(DcMotor.class, "bR");

        // 设置电机旋转的正方向。
        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftBackDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        rightBackDrive.setDirection(DcMotor.Direction.FORWARD);

        //imu读数初始化
        imu = hardwareMap.get(IMU.class, "imu");
        limelight=hardwareMap.get(Limelight3A.class,"limelight");
        limelight.pipelineSwitch(0);
        limelight.start();
        // 下面这段代码可以用来设置controlhub朝向，以正确使用imu
        imu.initialize(new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                )
        ));

        while (opModeInInit()) {

            //开启急停
            leftFrontDrive.getZeroPowerBehavior();
            rightFrontDrive.getZeroPowerBehavior();
            leftBackDrive.getZeroPowerBehavior();
            rightBackDrive.getZeroPowerBehavior();

            //可以直接telemetry.addData(“degree”, imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES))
            telemetry.addData(">", "Robot Heading = %4.0f",
                    imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES)
            );
            telemetry.update();
        }





        // 初始化结束，这里是等待DS上按下开始按钮。

        // 如果想要在初始化阶段显示一些信息，比如颜色传感器的读数，陀螺仪的角度等，
        // 可以用下面的代码代替上面那句 waitForStart();
        /*
        while (opModeInInit()) {
            telemetry.addData(">", "Waiting Time: " + runtime.toString());
            telemetry.update();
        }
        */
        //runtime.reset();

        // 下面的代码会不停循环直到DS上按下 STOP 按钮。
        while (opModeIsActive()) {


            LLResult result = limelight.getLatestResult();

            Pose3D botpose= result.getBotpose();
            telemetry.addData("X",result.getTx());
            telemetry.addData("Y",result.getTy());
            telemetry.addData("a",result.getTa());
            telemetry.addData("Botpose",botpose.toString());
            if(gamepad1.a){
                imu.resetYaw();
            }
            double max;

            // POV 模式，左摇杆前后左右平移，右摇杆转向。
            // axial 代表电机前进后退
            // lateral 代表电机左右平移
            // yaw 代表电机自转，顺时针或者逆时针
            double axial   =  -gamepad1.left_stick_y;  // 注意左摇杆往前推是负值
            double lateral =  gamepad1.left_stick_x;
            double yaw     =  -gamepad1.right_stick_x;

            // 把摇杆的读数进行线性组合，得到每个电机应当获得的功率值。
            double leftFrontPower  = axial + lateral + yaw;
            double rightFrontPower = axial - lateral - yaw;
            double leftBackPower   = axial - lateral + yaw;
            double rightBackPower  = axial + lateral - yaw;

            // 按照比例缩放每个电机功率，确保最大的那个电机功率不超过 100%
            max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
            max = Math.max(max, Math.abs(leftBackPower));
            max = Math.max(max, Math.abs(rightBackPower));

            if (max > 1.0) {
                leftFrontPower  /= max;
                rightFrontPower /= max;
                leftBackPower   /= max;
                rightBackPower  /= max;
            }
            /*leftFrontPower  /= 2;
            rightFrontPower /= 2;
            leftBackPower   /= 2;
            rightBackPower  /= 2;*/
            // 以下是测试电机的代码

            //***可以通过MecanumDirectionDebugger达到同样效果***

            // 把下方代码取消注释后，按ABXY几个按键，首先确认电机在Configuration里配置是否正确
            // 其次判断是否正确的旋转方向是否是向前的，用 setDirection() 来设置方向。
            // 一旦测试完毕需要把下方代码重新注释掉。


           /* leftFrontPower  = gamepad1.x ? 1.0 : 0.0;  // X gamepad
            leftBackPower   = gamepad1.a ? 1.0 : 0.0;  // A gamepad
            rightFrontPower = gamepad1.y ? 1.0 : 0.0;  // Y gamepad
            rightBackPower  = gamepad1.b ? 1.0 : 0.0;  // B gamepad
            */

            // 把各个电机计算出来的功率，应用到各个电机
            leftFrontDrive.setPower(leftFrontPower);
            rightFrontDrive.setPower(rightFrontPower);
            leftBackDrive.setPower(leftBackPower);
            rightBackDrive.setPower(rightBackPower);


            // 通过遥测功能，把当前运行状态发送给 Driver Station
            telemetry.addData("Front left/Right", "%4.2f, %4.2f", leftFrontPower, rightFrontPower);
            telemetry.addData("Back  left/Right", "%4.2f, %4.2f", leftBackPower, rightBackPower);
            telemetry.addData(">", "Robot Heading = %4.0f",
                    imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES)

            );
            telemetry.update();
        }
    }}
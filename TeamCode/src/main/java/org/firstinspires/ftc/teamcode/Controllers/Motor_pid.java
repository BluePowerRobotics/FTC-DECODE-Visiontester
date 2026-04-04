package org.firstinspires.ftc.teamcode.Controllers;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.utility.PIDController;

//单个弹射飞轮的PID控制器
@Config
public class Motor_pid {
    //被卡住的速度
    public static int SpeedTolerance = 35;
    //找到motor
    public DcMotorEx Motor_pid;

    //当前功率
    double Power = 0;
    //当前速度
    double current_speed = 0;
    //时间变量
    double current_time;
    double previous_time;
    //编码器变量
    double current_encoder = 0;
    //PID设置2套
    //PID切换速度
    public int PIDSwitchSpeed = 750;
    //PID参数(第一套
    public double MinPower_1 = 0.1;//最小功率
    public double k_p_1 = 0.01;
    public double k_i_1 = 0.2;
    public double k_d_1 = 0.05;
    public double max_i_1 = 1;
    //PID参数(第二套
    public double MinPower_2 = 0.3;
    public double k_p_2 = 0.011;
    public double k_d_2 = 0.08;
    public double k_i_2 = 0.05;
    public double max_i_2 = 0.6;
//加pid控制器
    private final PIDController pidController;

    //构造函数
    public Motor_pid(HardwareMap hardwareMap, String motorName, boolean ifReverse){
        Motor_pid = hardwareMap.get(DcMotorEx.class, motorName);
        Motor_pid.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        Motor_pid.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        Motor_pid.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        if(ifReverse)
            Motor_pid.setDirection(DcMotorSimple.Direction.REVERSE);
        else
            Motor_pid.setDirection(DcMotorSimple.Direction.FORWARD);
        pidController = new PIDController(k_p_1, k_i_1, k_d_1, max_i_1);
        previous_time = System.currentTimeMillis();
    }

    public void PIDsetting(int PIDSwitchSpeed,double MinPower_1,double k_p_1,double k_i_1,double k_d_1,double max_i_1,double MinPower_2 ,double k_p_2 ,double k_d_2 ,double k_i_2 , double max_i_2){
        this.PIDSwitchSpeed = PIDSwitchSpeed;
        this.MinPower_1 = MinPower_1;
        this.k_p_1 = k_p_1;
        this.k_i_1 = k_i_1;
        this.k_d_1 = k_d_1;
        this.max_i_1 = max_i_1;
        this.MinPower_2 = MinPower_2;
        this.k_p_2 = k_p_2;
        this.k_d_2 = k_d_2;
        this.k_i_2 = k_i_2;
        this.max_i_2 = max_i_2;
    }

    /**
     * 设置速度函数，使用PID控制电机速度
     * @param targetSpeed 目标速度
     *                    两套PID间隔为去configure里面找
     *
     * @return 是否达到目标速度
     *
     */
    public boolean setTargetSpeed(int targetSpeed){
        //根据速度选择PID参数
        if(targetSpeed <    PIDSwitchSpeed){
            pidController.setPID(k_p_1, k_i_1, k_d_1);
            pidController.setMaxI(max_i_1);
        }
        else{

            pidController.setPID(k_p_2, k_i_2, k_d_2);
            pidController.setMaxI(max_i_2);
        }
        //目标速度为0时，直接停止电机并重置PID
        if(targetSpeed == 0){
            Motor_pid.setPower(0);
            pidController.reset();
            previous_time = System.currentTimeMillis();
            return true;
        }
        //电机模式调整
        Motor_pid.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        current_time = System.currentTimeMillis();
        current_encoder = Motor_pid.getCurrentPosition();
        current_speed = Motor_pid.getVelocity(AngleUnit.DEGREES);
        double dt = (current_time - previous_time);
        if (dt <= 0) dt = 1; // 防止除零
        //计算PID
        Power = pidController.calculate(targetSpeed, current_speed, dt);
        //避免射球时出现负功率，导致震荡或电机损伤
        if(targetSpeed > 0 && targetSpeed < PIDSwitchSpeed){
            Power = Range.clip(Power, MinPower_1, 1);
        } else if(targetSpeed > 0 && targetSpeed >= PIDSwitchSpeed){
            Power = Range.clip(Power, MinPower_2, 1);
        }
        Motor_pid.setPower(Power);
        previous_time = current_time;
        if(Math.abs(targetSpeed - current_speed) < SpeedTolerance){
            return true;
        } else {
            return false;
        }
    }

    public double getCurrent_encoder(){
        return current_encoder;
    }
    public double getPower(){
        return Power;
    }
    public double getCurrent_speed(){
        return current_speed;
    }


}

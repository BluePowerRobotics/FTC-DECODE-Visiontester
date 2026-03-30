# FTC机器人项目完整代码结构说明

## 项目目录结构

```
Team19725_DECODE/
├── TeamCode/
│   ├── src/main/java/org/firstinspires/ftc/teamcode/
│   │   ├── OpModes/
│   │   │   ├── DECODE.java          # 主TeleOp程序
│   │   │   ├── FullPrompt.md        # 本文件，代码结构说明
│   │   │   ├── OpCode.md            # 操作说明文档
│   │   │   └── 其他测试OpMode文件
│   │   ├── RoadRunner/
│   │   │   ├── MecanumDrive.java    #  mecanum驱动实现
│   │   │   ├── Localizer.java       # 定位器接口
│   │   │   └── 其他RoadRunner相关文件
│   │   ├── Vision/
│   │   │   └── AprilTagDetector.java # AprilTag视觉检测
│   │   ├── controllers/
│   │   │   ├── chassis/
│   │   │   │   ├── ChassisController.java # 底盘控制器
│   │   │   │   └── locate/RobotPosition.java # 机器人位置管理
│   │   │   ├── shooter/
│   │   │   │   ├── Shooter.java     # 单个发射器控制器
│   │   │   │   ├── ShooterAction.java # 发射器动作控制器
│   │   │   │   └── DRL/Agent.java   # DRL代理实现
│   │   │   ├── Sweeper/
│   │   │   │   └── Sweeper_PID.java # 清扫器控制器
│   │   │   ├── elevator/
│   │   │   │   └── ElevatorController.java # 电梯控制器
│   │   │   ├── BlinkinLedController.java # LED控制器
│   │   │   ├── Trigger.java         # 触发器控制器
│   │   │   └── 其他传感器控制器
│   │   ├── utility/
│   │   │   ├── ActionRunner.java    # 动作运行器
│   │   │   ├── PIDController.java   # PID控制器实现
│   │   │   ├── MathSolver.java      # 数学工具类
│   │   │   └── solvepoint/
│   │   │       ├── SolveShootPoint.java # 射击点计算
│   │   │       ├── SolveEatPoint.java   # 取球点计算
│   │   │       └── SolveClimbPoint.java # 攀爬点计算
```

## 核心文件说明

### 1. DECODE.java

**功能**：主TeleOp程序，负责整体机器人控制，包括模式切换、DRL集成、游戏手柄输入处理等。

**结构**：
- 枚举类型：ROBOT_STATUS、TEAM_COLOR、TRIGGER_STATUS、SWEEPER_STATUS、SHOOTER_STATUS、DRL_MODE
- 成员变量：各种控制器实例、状态变量、参数配置
- 方法：
  - Init()：初始化硬件和控制器
  - calculateShootSpeed()：根据DRL模式计算发射速度
  - calculateShootSpeedWithDRL()：使用DRL计算发射速度
  - inputRobotStatus()：处理游戏手柄输入，更新机器人状态
  - setStatus()：根据机器人状态设置各子系统状态
  - Telemetry()：显示遥测数据
  - shoot()：处理发射逻辑
  - chassis()：处理底盘控制
  - sweeper()：处理清扫器控制
  - runOpMode()：主运行循环

**关键特性**：
- 三种DRL模式：FullDRL、RunDRL、NoDRL
- 速度阈值检测（RunDRL模式）
- 自动航向锁定
- 无头模式切换
- 游戏手柄输入处理
- 遥测数据显示

### 2. ChassisController.java

**功能**：底盘控制器，负责机器人的移动控制、位置管理和导航。

**结构**：
- 内部类：Params（参数配置）、ChassisCalculator（底盘计算工具）
- 成员变量：robotPosition、hardwareMap、状态变量、PID控制器
- 方法：
  - 构造函数：初始化底盘控制器
  - exchangeNoHeadMode()：切换无头模式
  - setAutoLockHeading()：设置自动航向锁定
  - setHeadingLockRadian()：设置航向锁定角度
  - setTargetPoint()：设置目标点导航
  - getVelocityX()/getVelocityY()：获取机器人速度
  - gamepadInput()：处理游戏手柄输入

**关键特性**：
- 支持无头模式和机器人模式
- 自动航向锁定
- 目标点导航
- 速度获取（用于DRL模式）
- PID航向控制

### 3. ShooterAction.java

**功能**：控制双发射轮的共同运作和自动化动作。

**结构**：
- 成员变量：发射器实例、速度配置、DRL相关参数
- 内部类：SpeedUp、ShootThreeArtifacts、SpeedUpWithDRL、ShootThreeArtifactsWithDRL（动作实现）
- 方法：
  - setShootSpeed()：设置发射速度
  - setShootSpeedWithDRL()：使用DRL设置发射速度
  - getPower1()/getPower2()：获取电机功率
  - getCurrent_speed1()/getCurrent_speed2()：获取当前速度
  - setTurretAngle()：设置炮塔角度
  - setUseDRL()：设置是否使用DRL

**关键特性**：
- 双发射轮控制
- DRL集成
- 自动化发射动作
- 速度检测和控制

### 4. Shooter.java

**功能**：单个弹射飞轮的PID控制器，包含DRL模型集成。

**结构**：
- 成员变量：电机实例、PID控制器、DRL代理、状态变量
- 方法：
  - loadDRLModel()：加载DRL模型
  - calculateDRLSpeed()：使用DRL计算发射速度
  - calculateLaunchSpeedFromVector()：从向量计算发射速度
  - shoot()：PID控制发射速度
  - shootWithDRL()：使用DRL控制发射速度
  - isDRLModelLoaded()：检查DRL模型是否加载
  - setUseDRLModel()：设置是否使用DRL模型

**关键特性**：
- PID速度控制（两套参数）
- DRL模型集成
- 速度检测和反馈

### 5. SolveShootPoint.java

**功能**：工具类，用于计算射击点位置和速度。

**结构**：
- 静态变量：距离参数、DRL相关变量
- 静态方法：
  - initDRL()：初始化DRL
  - loadDRLModel()：加载DRL模型
  - setUseDRLModel()：设置是否使用DRL模型
  - solveShootSpeed()：计算发射速度（支持传统方法和DRL）
  - solveShootSpeedWithDRL()：使用DRL计算发射速度
  - solveBLUEShootHeading()/solveREDShootHeading()：计算射击角度
  - solveBLUEShootDistance()/solveREDShootDistance()：计算射击距离
  - solveBLUEShootPoint()/solveREDShootPoint()：计算射击位置

**关键特性**：
- 支持红蓝双方场地
- DRL集成
- 传统方法备份
- 特殊情况处理（小三角区域）

## 代码关系与依赖

1. **DECODE.java** 依赖所有控制器和工具类：
   - ChassisController：底盘控制
   - ShooterAction：发射控制
   - Sweeper_PID：清扫器控制
   - ElevatorController：电梯控制
   - Trigger：触发器控制
   - SolveShootPoint：射击点计算
   - ActionRunner：动作执行

2. **ChassisController** 依赖：
   - RobotPosition：位置管理
   - PIDController：PID控制
   - MathSolver：数学工具

3. **ShooterAction** 依赖：
   - Shooter：单个发射器控制
   - MeanFilter：速度滤波

4. **Shooter** 依赖：
   - Agent：DRL代理
   - PIDController：PID控制

5. **SolveShootPoint** 依赖：
   - Agent：DRL代理

## 关键算法与技术

### 1. DRL（深度强化学习）集成

- **输入**：机器人速度向量（vx, vy）、目标相对位置（x, y）
- **输出**：发射初速度（三维向量）
- **模型加载**：从/sdcard/FIRST/agent_DDPG.ser加载
- **模式**：
  - FullDRL：始终使用DRL
  - RunDRL：速度超过阈值时使用DRL
  - NoDRL：使用传统方法

### 2. PID控制

- **底盘航向控制**：双PID参数（强力模式和普通模式）
- **发射器速度控制**：两套参数（大三角和小三角）
- **参数自适应**：根据目标速度自动切换参数

### 3. 路径规划与导航

- **RoadRunner**：使用RoadRunner库进行路径规划
- **目标点导航**：自动计算并导航到射击位置
- **位置更新**：实时更新机器人位置

### 4. 状态机管理

- **机器人状态**：EATING、WAITING、OUTPUTTING、SHOOTING、CLIMBING
- **子系统状态**：清扫器、发射器、触发器状态
- **状态切换**：通过游戏手柄输入和条件判断切换状态

## 操作说明

### 游戏手柄控制

**Gamepad1（一操）**：
- 左摇杆：移动控制
- 右摇杆：旋转控制
- 左扳机：自动对准射击角度
- 右扳机：自动锁定航向
- A键：切换到蓝色队伍
- B键：切换到红色队伍 / 进入/退出攀爬模式
- X键：切换无头模式
- Y键：准备射击 / 取消准备
- 左 bumper：进入取球模式
- 右 bumper：进入输出模式
- D-pad上：导航到远侧射击点
- D-pad下：导航到近侧射击点
- D-pad左：计算并设置发射速度
- D-pad右：增加角度容差

**Gamepad2（二操）**：
- 左摇杆：辅助移动
- 右摇杆：辅助旋转
- A键：切换到等待模式
- B键：切换DRL模式（NoDRL → FullDRL → RunDRL → NoDRL）
- X键：重置为等待模式
- Y键：进入射击模式
- 左 bumper：切换取球模式
- 右 bumper：切换输出模式
- 左扳机：电梯向下
- 右扳机：电梯向上
- D-pad左：减少角度偏移
- D-pad右：增加角度偏移
- D-pad下：减少速度系数
- D-pad上：增加速度系数
- 开始键：强制切换发射状态

### DRL模式说明

- **NoDRL**：使用传统方法计算发射速度，基于距离的线性公式
- **FullDRL**：始终使用DRL模型计算发射速度，考虑机器人速度和目标位置
- **RunDRL**：当机器人速度超过0.1m/s时使用DRL，否则使用传统方法

## 配置与参数

### 底盘参数（ChassisController.Params）
- maxV：最大线速度（m/s）
- maxOmega：最大角速度（rad/s）
- zeroThresholdV：速度零点阈值（m/s）
- zeroThresholdOmega：角速度零点阈值（rad/s）
- FrontToCenterInch：前轮到中心距离（英寸）
- SideToCenterInch：侧轮到中心距离（英寸）

### 发射器参数（ShooterAction）
- speed25_25：近距离发射速度
- speed25_55：中距离发射速度
- speed35_55：远距离发射速度
- speedDescend：速度下降阈值
- waitTime：第一个球推入时间（ms）
- ShootTime：总射击时间（ms）

### PID参数（Shooter）
- k_p/k_i/k_d：大三角PID参数
- k_p_small/k_i_small/k_d_small：小三角PID参数
- SpeedTolerance：速度容差

### DRL参数（DECODE）
- drlSpeedThreshold：DRL启用速度阈值（m/s）

## 硬件配置

### 电机
- shooterMotor1：左发射器电机
- shooterMotor2：右发射器电机
- sweeperMotor：清扫器电机
- 底盘电机：四个驱动电机（mecanum）
- elevatorMotor：电梯电机

### 传感器
- IMU：惯性测量单元（用于定位和姿态）
- 编码器：驱动轮编码器（用于位置跟踪）
- 距离传感器：可选（用于检测球）

### 其他硬件
- Blinkin LED：LED控制器
- 触发器：控制球的发射

## 代码生成提示

要重新生成此项目的代码，请确保：

1. **目录结构**：按照上述目录结构创建文件
2. **依赖项**：
   - RoadRunner库
   - FTC SDK
   - DRL模型文件（agent_DDPG.ser）
3. **硬件映射**：根据实际硬件配置修改电机和传感器名称
4. **参数调整**：根据机器人实际情况调整PID参数和速度阈值
5. **DRL集成**：确保Agent类正确实现，并且模型文件路径正确

## 测试与调试

1. **DRL模型加载**：检查telemetry输出，确认模型是否成功加载
2. **速度控制**：使用FtcDashboard监控发射速度和PID输出
3. **位置跟踪**：验证RoadRunner定位是否准确
4. **模式切换**：测试三种DRL模式的切换是否正常
5. **自动导航**：测试导航到射击点的准确性

## 总结

本项目实现了一个功能完整的FTC机器人控制程序，集成了深度强化学习（DRL）技术用于优化发射速度计算。主要特点包括：

- 灵活的DRL模式切换（FullDRL、RunDRL、NoDRL）
- 精确的PID速度控制
- 自动导航和航向锁定
- 完整的状态机管理
- 详细的遥测数据
- 模块化的代码结构

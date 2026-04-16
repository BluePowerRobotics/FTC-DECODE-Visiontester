# Tracker 类设计文档

## 概述

`Tracker` 用于管理从 Limelight 神经检测器获取的多个小球检测结果。它负责将空间上靠近的小球聚类为同一个目标（Target），并对检测结果的时序波动（突然出现或消失）进行平滑处理，最终提供一个最优目标用于机器人控制。

***

## 1. 内部类 `Target`

### 属性

| 名称                  | 类型               | 说明                           |
| ------------------- | ---------------- | ---------------------------- |
| `id`                | int              | 唯一标识符                        |
| `memberDetections`  | List\<Detection> | 当前属于该目标的原始检测框列表              |
| `centerX`           | double           | 目标中心 X 坐标（所有成员坐标的平均值，世界坐标单位） |
| `centerY`           | double           | 目标中心 Y 坐标                    |
| `lastSeenTimestamp` | long             | 最后一次有成员更新的时间（毫秒）             |
| `stableCount`       | int              | 连续稳定存在的帧数（用于判断是否真正消失）        |

### 方法

- `updateCenter()`：根据成员坐标重新计算中心坐标。
- `addDetection(Detection d)`：添加一个检测框，并更新中心。
- `removeDetection(Detection d)`：移除一个检测框，并更新中心。
- `isEmpty()`：判断成员列表是否为空。
- `getDistanceToCamera()`：计算目标中心到摄像头的距离（根据世界坐标模长）。

***

## 2. `Tracker` 主类

### 成员变量

| 名称                   | 类型                       | 说明                             |
| -------------------- | ------------------------ | ------------------------------ |
| `targets`            | List\<Target>            | 存储所有当前有效目标                     |
| `nextTargetId`       | int                      | 用于分配新目标的唯一 ID                  |
| `detectionHistory`   | Map\<Detection, Integer> | 记录临时检测框的连续出现帧数（用于确认新目标）        |
| `removalPending`     | Map\<Target, Integer>    | 记录待移除目标的连续消失帧数                 |
| `distanceThreshold`  | double                   | 两小球归为同一组的最大距离（世界坐标，单位与摄像头标定一致） |
| `confirmationFrames` | int                      | 新目标需要连续出现的帧数（默认 5 帧）           |
| `removalFrames`      | int                      | 目标需要连续消失的帧数（默认 5 帧）            |
| `cameraCalibration`  | CameraCalibration        | 摄像头标定参数（用于像素坐标 -> 世界坐标换算）      |

### 主要方法

#### 2.1 更新接口

```java
public void update(List<Detection> currentDetections)
```

- **功能**：每一帧调用，输入当前帧的所有检测框（已过滤置信度）。执行以下子步骤：
  1. 将像素坐标转换为世界坐标（相对于摄像头，Z 轴为距离）。
  2. 对检测框进行空间聚类（基于 `distanceThreshold`），得到若干组。
  3. 与现有的 `targets` 进行匹配，更新或创建/删除目标。
  4. 处理时序平滑（出现/消失的持久化）。

#### 2.2 空间聚类

```java
private List<List<Detection>> clusterDetections(List<Detection> detections)
```

- **功能**：将传入的检测列表按两两之间的距离（`distanceThreshold`）进行聚类。使用并查集或贪心算法，所有互相距离 ≤ 阈值的检测归为同一组。
- **返回**：一组组的检测列表。

#### 2.3 匹配与更新

```java
private void matchAndUpdateTargets(List<List<Detection>> currentGroups)
```

- **功能**：将当前帧的聚类组与现有 `targets` 进行关联。
  - 对每个 `currentGroup`，计算其中心，并与现有 `targets` 的中心距离比较，若距离 ≤ `distanceThreshold`，则认为匹配。
  - 匹配到的组：用该组更新对应 `Target` 的成员列表（完全替换，而非增量）。
  - 未匹配到的组：进入“候选”状态（见 2.4）。
  - 现有 `targets` 中未被匹配到的：进入“待移除”状态（见 2.5）。

#### 2.4 新目标确认（出现平滑）

```java
private void confirmNewCandidates(List<Detection> ungroupedDetections)
```

- **功能**：对未匹配到现有目标的检测组，不立即创建 `Target`，而是将其视为候选。利用 `detectionHistory` 记录该组中心（或成员组合）连续出现的次数。若连续出现 ≥ `confirmationFrames` 帧，则创建新的 `Target` 并加入 `targets`。

#### 2.5 旧目标移除（消失平滑）

```java
private void handleMissingTargets()
```

- **功能**：对本次未被匹配的现有 `targets`，不立即删除。利用 `removalPending` 记录其连续缺失的帧数。若连续缺失 ≥ `removalFrames` 帧，则从 `targets` 中移除该 `Target`。若中途重新出现，则重置计数。

#### 2.6 最优目标选择

```java
public Target getBestTarget()
```

- **功能**：根据规则计算每个 `Target` 的得分，返回得分最高的 `Target`。若没有有效目标，返回 `null`。
- **得分公式**：`score = memberCount / sqrt(centerX² + centerY² + Z²)`，其中 Z 为距离摄像头的深度（可从世界坐标计算）。即**包含小球个数越多、离摄像头越近的目标得分越高**。

#### 2.7 辅助方法

```java
private void removeTarget(Target target)
```

- 从 `targets` 列表中移除指定目标，并清理相关的时序记录。

```java
private void addTarget(Target target)
```

- 将新创建的目标加入 `targets`，并分配 ID。

```java
private double computeTargetScore(Target target)
```

- 计算单个目标的得分（用于 `getBestTarget`）。

***

## 3. 依赖的外部类说明

- `Detector`：负责从Limelight获取检测结果
- `Projection`：负责将画面坐标转换为相对于摄像头的现实坐标

***

## 4. 使用流程示例

```java
// 初始化
Tracker tracker = new Tracker(distanceThreshold, confirmationFrames, removalFrames, calibration);

// 每帧循环
while (true) {
    List<Detection> detections = limelight.getDetectorResults(); // 获取当前帧检测
    tracker.update(detections);
    Target best = tracker.getBestTarget();
    if (best != null) {
        // 输出最佳目标的世界坐标 (best.centerX, best.centerY, best.getDistanceToCamera())
    }
}
```

***

## 5. 设计要点总结

- **空间聚类**：将物理距离近的小球合并为同一个目标（例如一簇球）。
- **时序平滑**：避免因单帧检测丢失或闪烁导致目标频繁变动，提高稳定性。
- **最优选择**：鼓励靠近摄像头且包含更多小球的团簇，便于机器人决策。
- **可配置参数**：距离阈值、确认帧数、移除帧数可根据实际场景调优。


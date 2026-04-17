package org.firstinspires.ftc.teamcode.Vision.projection;

import java.util.HashMap;
import java.util.Map;

public class Projector {
    private static final double rt2 = Math.sqrt(2);

    private static final Map<String, Double> CAMERA_PHI_MAP = new HashMap<>();
    static {
        CAMERA_PHI_MAP.put("Limelight", 0.0);
    }

    private final double m0;
    private final double h;

    public Projector(double m0, double h) {
        this.m0 = m0;
        this.h = h;
    }

    public double[] project(double m, double n) {
        double d = (rt2 * m0 * h) / (m0 - n);
        double delta_x = rt2 * d - h;
        double delta_y = m * d / m0;
        return new double[]{delta_x, delta_y};
    }

    public double[] project(double m, double n, double theta) {
        double[] pos = project(m, n);
        double rotationAngle = theta;
        double rotatedX = pos[0] * Math.cos(rotationAngle) - pos[1] * Math.sin(rotationAngle);
        double rotatedY = pos[0] * Math.sin(rotationAngle) + pos[1] * Math.cos(rotationAngle);
        return new double[]{rotatedX, rotatedY};
    }

    public static void addCameraPhi(String cameraName, double phi) {
        CAMERA_PHI_MAP.put(cameraName, phi);
    }

    public static double getCameraPhi(String cameraName) {
        return CAMERA_PHI_MAP.getOrDefault(cameraName, 0.0);
    }
}
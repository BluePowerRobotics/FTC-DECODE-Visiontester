package org.firstinspires.ftc.teamcode.Controllers.Limelight;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.Vision.projection.Projector;

import java.util.*;

public class Tracker {
    private Detector detector;
    private Projector projector = new Projector(0.9, 0.1);
    private List<Target> targets;
    private List<CandidateTarget> candidateTargets;
    private int nextTargetId;
    private Map<Target, Integer> removalPending;
    private double distanceThreshold;
    private int confirmationFrames;
    private int removalFrames;

    public Tracker(HardwareMap hardwareMap, double distanceThreshold, int confirmationFrames, int removalFrames) {
        this.detector = new Detector(hardwareMap);
        this.targets = new ArrayList<>();
        this.candidateTargets = new ArrayList<>();
        this.nextTargetId = 0;
        this.removalPending = new HashMap<>();
        this.distanceThreshold = distanceThreshold;
        this.confirmationFrames = confirmationFrames;
        this.removalFrames = removalFrames;
    }

    public void start() {
        detector.start();
    }

    public void update() {
        List<Detection> currentDetections = getCurrentDetections();
        List<List<Detection>> currentGroups = clusterDetections(currentDetections);
        matchAndUpdateTargets(currentGroups);
        handleNewCandidates(currentGroups);
        handleMissingTargets();
    }

    private List<Detection> getCurrentDetections() {
        List<Detection> detections = new ArrayList<>();

        double[][] purpleOffsets = detector.get_center("purple");
        for (double[] offset : purpleOffsets) {
            double[] worldPos = projector.project(offset[0], offset[1]);
            detections.add(new Detection("purple", worldPos[0], worldPos[1]));
        }

        double[][] greenOffsets = detector.get_center("green");
        for (double[] offset : greenOffsets) {
            double[] worldPos = projector.project(offset[0], offset[1]);
            detections.add(new Detection("green", worldPos[0], worldPos[1]));
        }

        return detections;
    }

    private List<List<Detection>> clusterDetections(List<Detection> detections) {
        List<List<Detection>> groups = new ArrayList<>();
        Set<Detection> processed = new HashSet<>();

        for (Detection detection : detections) {
            if (processed.contains(detection)) {
                continue;
            }

            List<Detection> group = new ArrayList<>();
            Queue<Detection> queue = new LinkedList<>();

            group.add(detection);
            queue.add(detection);
            processed.add(detection);

            while (!queue.isEmpty()) {
                Detection current = queue.poll();
                for (Detection other : detections) {
                    if (!processed.contains(other) && current.distanceTo(other) <= distanceThreshold) {
                        group.add(other);
                        queue.add(other);
                        processed.add(other);
                    }
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private void matchAndUpdateTargets(List<List<Detection>> currentGroups) {
        Set<Target> matchedTargets = new HashSet<>();

        for (List<Detection> group : currentGroups) {
            double centerX = 0, centerY = 0;
            for (Detection detection : group) {
                centerX += detection.x;
                centerY += detection.y;
            }
            centerX /= group.size();
            centerY /= group.size();

            Target matchedTarget = null;
            double minDistance = Double.MAX_VALUE;

            for (Target target : targets) {
                if (matchedTargets.contains(target)) continue;

                double distance = Math.sqrt(Math.pow(target.centerX - centerX, 2) + Math.pow(target.centerY - centerY, 2));
                if (distance <= distanceThreshold && distance < minDistance) {
                    matchedTarget = target;
                    minDistance = distance;
                }
            }

            if (matchedTarget != null) {
                matchedTarget.updateMembers(group);
                matchedTarget.lastSeenTimestamp = System.currentTimeMillis();
                matchedTargets.add(matchedTarget);
                removalPending.remove(matchedTarget);
            }
        }

        for (Target target : targets) {
            if (!matchedTargets.contains(target)) {
                int count = removalPending.getOrDefault(target, 0) + 1;
                removalPending.put(target, count);
            }
        }
    }

    private void handleNewCandidates(List<List<Detection>> currentGroups) {
        Set<CandidateTarget> matchedCandidates = new HashSet<>();
        List<CandidateTarget> toRemove = new ArrayList<>();

        for (List<Detection> group : currentGroups) {
            double centerX = 0, centerY = 0;
            for (Detection detection : group) {
                centerX += detection.x;
                centerY += detection.y;
            }
            centerX /= group.size();
            centerY /= group.size();

            CandidateTarget matchedCandidate = null;
            double minDistance = Double.MAX_VALUE;

            for (CandidateTarget candidate : candidateTargets) {
                double distance = Math.hypot(candidate.centerX - centerX, candidate.centerY - centerY);
                if (distance <= distanceThreshold && distance < minDistance) {
                    matchedCandidate = candidate;
                    minDistance = distance;
                }
            }

            if (matchedCandidate != null) {
                matchedCandidate.updateMembers(group);
                matchedCandidate.consecutiveFrames++;
                matchedCandidate.consecutiveMisses = 0;
                matchedCandidates.add(matchedCandidate);

                if (matchedCandidate.consecutiveFrames >= confirmationFrames) {
                    Target newTarget = new Target(nextTargetId++);
                    newTarget.updateMembers(matchedCandidate.memberDetections);
                    targets.add(newTarget);
                    toRemove.add(matchedCandidate);
                }
            } else {
                CandidateTarget newCandidate = new CandidateTarget(group);
                candidateTargets.add(newCandidate);
            }
        }

        for (CandidateTarget candidate : candidateTargets) {
            if (!matchedCandidates.contains(candidate)) {
                candidate.consecutiveMisses++;
                candidate.consecutiveFrames = 0;
            }
        }

        List<CandidateTarget> toRemoveFinal = new ArrayList<>();
        for (CandidateTarget candidate : candidateTargets) {
            if (candidate.consecutiveMisses >= removalFrames) {
                toRemoveFinal.add(candidate);
            }
        }
        toRemoveFinal.addAll(toRemove);
        candidateTargets.removeAll(toRemoveFinal);
    }

    private void handleMissingTargets() {
        List<Target> toRemove = new ArrayList<>();

        for (Map.Entry<Target, Integer> entry : removalPending.entrySet()) {
            Target target = entry.getKey();
            int count = entry.getValue();

            if (count >= removalFrames) {
                toRemove.add(target);
            }
        }

        for (Target target : toRemove) {
            targets.remove(target);
            removalPending.remove(target);
        }
    }

    public Target getBestTarget() {
        if (targets.isEmpty()) {
            return null;
        }

        Target bestTarget = null;
        double bestScore = -1;

        for (Target target : targets) {
            double score = computeTargetScore(target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private double computeTargetScore(Target target) {
        int memberCount = target.memberDetections.size();
        double distance = Math.sqrt(Math.pow(target.centerX, 2) + Math.pow(target.centerY, 2));
        if (distance == 0) {
            distance = 0.1;
        }
        return memberCount / distance;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public List<CandidateTarget> getCandidateTargets() {
        return candidateTargets;
    }

    public void stop() {
        detector.stop();
    }

    public static class Detection {
        public final String color;
        public final double x;
        public final double y;

        public Detection(String color, double x, double y) {
            this.color = color;
            this.x = x;
            this.y = y;
        }

        public double distanceTo(Detection other) {
            return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Detection that = (Detection) o;
            return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Objects.equals(color, that.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(color, x, y);
        }
    }

    public static class Target {
        public int id;
        public List<Detection> memberDetections;
        public double centerX;
        public double centerY;
        public long lastSeenTimestamp;

        public Target(int id) {
            this.id = id;
            this.memberDetections = new ArrayList<>();
            this.centerX = 0;
            this.centerY = 0;
            this.lastSeenTimestamp = System.currentTimeMillis();
        }

        public void updateMembers(List<Detection> detections) {
            this.memberDetections = new ArrayList<>(detections);
            updateCenter();
        }

        public void updateCenter() {
            if (memberDetections.isEmpty()) {
                centerX = 0;
                centerY = 0;
                return;
            }

            double sumX = 0, sumY = 0;
            for (Detection detection : memberDetections) {
                sumX += detection.x;
                sumY += detection.y;
            }

            centerX = sumX / memberDetections.size();
            centerY = sumY / memberDetections.size();
        }

        public boolean isEmpty() {
            return memberDetections.isEmpty();
        }

        public double getDistanceToCamera() {
            return Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
        }
    }

    public static class CandidateTarget {
        public List<Detection> memberDetections;
        public double centerX;
        public double centerY;
        public int consecutiveFrames;
        public int consecutiveMisses;

        public CandidateTarget(List<Detection> detections) {
            this.memberDetections = new ArrayList<>(detections);
            this.consecutiveFrames = 1;
            this.consecutiveMisses = 0;
            updateCenter();
        }

        public void updateMembers(List<Detection> detections) {
            this.memberDetections = new ArrayList<>(detections);
            updateCenter();
        }

        public void updateCenter() {
            if (memberDetections.isEmpty()) {
                centerX = 0;
                centerY = 0;
                return;
            }

            double sumX = 0, sumY = 0;
            for (Detection detection : memberDetections) {
                sumX += detection.x;
                sumY += detection.y;
            }

            centerX = sumX / memberDetections.size();
            centerY = sumY / memberDetections.size();
        }
    }
}
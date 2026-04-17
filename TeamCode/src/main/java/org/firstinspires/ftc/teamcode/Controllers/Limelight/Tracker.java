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
    private static int nextMemberId;
    private Map<Target, Integer> removalPending;
    private double distanceThreshold;
    private int confirmationFrames;
    private int removalFrames;

    public Tracker(HardwareMap hardwareMap, double distanceThreshold, int confirmationFrames, int removalFrames) {
        this.detector = new Detector(hardwareMap);
        this.targets = new ArrayList<>();
        this.candidateTargets = new ArrayList<>();
        this.nextTargetId = 0;
        nextMemberId = 0;
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
                matchedTarget.updateGroupAndMembers(group, confirmationFrames);
                matchedTarget.lastSeenTimestamp = System.currentTimeMillis();
                matchedTargets.add(matchedTarget);
                removalPending.remove(matchedTarget);
            }
        }

        for (Target target : targets) {
            if (!matchedTargets.contains(target)) {
                target.markAllMembersMissed();
                int count = removalPending.getOrDefault(target, 0) + 1;
                removalPending.put(target, count);
            }
        }

        List<Target> toCheckEmpty = new ArrayList<>(targets);
        for (Target target : toCheckEmpty) {
            target.removeStaleMembers(removalFrames, confirmationFrames);
            if (target.isEmpty()) {
                removalPending.put(target, removalFrames);
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
                matchedCandidate.updateGroupAndMembers(group);
                matchedCandidate.consecutiveFrames++;
                matchedCandidate.consecutiveMisses = 0;
                matchedCandidates.add(matchedCandidate);

                if (matchedCandidate.consecutiveFrames >= confirmationFrames) {
                    Target newTarget = new Target(nextTargetId++);
                    newTarget.updateGroupAndMembers(matchedCandidate.members, confirmationFrames);
                    targets.add(newTarget);
                    toRemove.add(matchedCandidate);
                }
            } else {
                CandidateTarget newCandidate = new CandidateTarget(nextMemberId++, group);
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

            if (count >= removalFrames && target.getActiveMemberCount(confirmationFrames) == 0) {
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
            if (target.isEmpty()) continue;
            if (target.getActiveMemberCount(confirmationFrames) == 0) continue;
            double score = computeTargetScore(target);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private double computeTargetScore(Target target) {
        int memberCount = target.getActiveMemberCount(confirmationFrames);
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
        public Map<Integer, Member> members;
        public double centerX;
        public double centerY;
        public long lastSeenTimestamp;

        public Target(int id) {
            this.id = id;
            this.members = new HashMap<>();
            this.centerX = 0;
            this.centerY = 0;
            this.lastSeenTimestamp = System.currentTimeMillis();
        }

        public void updateGroupAndMembers(List<Detection> group, int confirmationFrames) {
            Set<Integer> processedMemberIds = new HashSet<>();

            for (Detection detection : group) {
                Member matchedMember = null;
                double minDistance = Double.MAX_VALUE;

                for (Map.Entry<Integer, Member> entry : members.entrySet()) {
                    Member member = entry.getValue();
                    if (member.detection.color.equals(detection.color)) {
                        double distance = member.detection.distanceTo(detection);
                        if (distance < minDistance) {
                            minDistance = distance;
                            matchedMember = member;
                        }
                    }
                }

                if (matchedMember != null && minDistance <= 0.3) {
                    matchedMember.consecutiveFrames++;
                    matchedMember.consecutiveMisses = 0;
                    matchedMember.detection = detection;
                    processedMemberIds.add(matchedMember.id);
                } else {
                    Member newMember = new Member(Tracker.nextMemberId++, detection);
                    members.put(newMember.id, newMember);
                    processedMemberIds.add(newMember.id);
                }
            }

            for (Map.Entry<Integer, Member> entry : members.entrySet()) {
                if (!processedMemberIds.contains(entry.getKey())) {
                    Member m = entry.getValue();
                    m.consecutiveMisses++;
                    m.consecutiveFrames = 0;
                }
            }

            updateCenter(confirmationFrames);
        }

        public void updateGroupAndMembers(Map<Integer, Member> existingMembers, int confirmationFrames) {
            this.members = new HashMap<>(existingMembers);
            updateCenter(confirmationFrames);
        }

        public void markAllMembersMissed() {
            for (Member member : members.values()) {
                member.consecutiveMisses++;
                member.consecutiveFrames = 0;
            }
        }

        public void removeStaleMembers(int removalFrames, int confirmationFrames) {
            List<Integer> toRemove = new ArrayList<>();
            for (Map.Entry<Integer, Member> entry : members.entrySet()) {
                if (entry.getValue().consecutiveMisses >= removalFrames) {
                    toRemove.add(entry.getKey());
                }
            }
            for (Integer id : toRemove) {
                members.remove(id);
            }
            updateCenter(confirmationFrames);
        }

        public int getActiveMemberCount(int confirmationFrames) {
            int count = 0;
            for (Member member : members.values()) {
                if (member.consecutiveFrames >= confirmationFrames) {
                    count++;
                }
            }
            return count;
        }

        public void updateCenter(int confirmationFrames) {
            int activeCount = 0;
            double sumX = 0, sumY = 0;
            for (Member member : members.values()) {
                if (member.consecutiveFrames >= confirmationFrames) {
                    sumX += member.detection.x;
                    sumY += member.detection.y;
                    activeCount++;
                }
            }

            if (activeCount == 0) {
                centerX = 0;
                centerY = 0;
            } else {
                centerX = sumX / activeCount;
                centerY = sumY / activeCount;
            }
        }

        public boolean isEmpty() {
            return members.isEmpty();
        }

        public double getDistanceToCamera() {
            return Math.sqrt(Math.pow(centerX, 2) + Math.pow(centerY, 2));
        }

        public static class Member {
            public int id;
            public Detection detection;
            public int consecutiveFrames;
            public int consecutiveMisses;

            public Member(int id, Detection detection) {
                this.id = id;
                this.detection = detection;
                this.consecutiveFrames = 1;
                this.consecutiveMisses = 0;
            }
        }
    }

    public static class CandidateTarget {
        public Map<Integer, Target.Member> members;
        public double centerX;
        public double centerY;
        public int consecutiveFrames;
        public int consecutiveMisses;
        private int nextMemberId;

        public CandidateTarget(int startMemberId, List<Detection> group) {
            this.members = new HashMap<>();
            this.nextMemberId = startMemberId;
            for (Detection detection : group) {
                Target.Member member = new Target.Member(Tracker.nextMemberId++, detection);
                members.put(member.id, member);
            }
            this.consecutiveFrames = 1;
            this.consecutiveMisses = 0;
            updateCenter();
        }

        public void updateGroupAndMembers(List<Detection> group) {
            Set<Integer> processedMemberIds = new HashSet<>();

            for (Detection detection : group) {
                Target.Member matchedMember = null;
                double minDistance = Double.MAX_VALUE;

                for (Map.Entry<Integer, Target.Member> entry : members.entrySet()) {
                    Target.Member member = entry.getValue();
                    if (member.detection.color.equals(detection.color)) {
                        double distance = member.detection.distanceTo(detection);
                        if (distance < minDistance) {
                            minDistance = distance;
                            matchedMember = member;
                        }
                    }
                }

                if (matchedMember != null && minDistance <= 0.3) {
                    matchedMember.consecutiveFrames++;
                    matchedMember.consecutiveMisses = 0;
                    matchedMember.detection = detection;
                    processedMemberIds.add(matchedMember.id);
                } else {
                    Target.Member newMember = new Target.Member(Tracker.nextMemberId++, detection);
                    members.put(newMember.id, newMember);
                    processedMemberIds.add(newMember.id);
                }
            }

            for (Map.Entry<Integer, Target.Member> entry : members.entrySet()) {
                if (!processedMemberIds.contains(entry.getKey())) {
                    Target.Member m = entry.getValue();
                    m.consecutiveMisses++;
                    m.consecutiveFrames = 0;
                }
            }

            updateCenter();
        }

        private void updateCenter() {
            if (members.isEmpty()) {
                centerX = 0;
                centerY = 0;
                return;
            }

            double sumX = 0, sumY = 0;
            for (Target.Member member : members.values()) {
                sumX += member.detection.x;
                sumY += member.detection.y;
            }

            centerX = sumX / members.size();
            centerY = sumY / members.size();
        }
    }
}
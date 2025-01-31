package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FusionSlamTest {

   private FusionSlam fusionSlam;

   @BeforeEach
   void setUp() {
      fusionSlam = FusionSlam.getInstance();
      fusionSlam.clearForDebug();
   }

   /**
    * Test: Adds poses and retrieves them correctly.
    * Pre-Condition: FusionSlam instance is initialized.
    * Post-Condition: Stored poses match the added poses.
    * Invariant: Pose list size increases as poses are added.
    */
   @Test
   void testAddPoseAndRetrieve() {
      Pose pose1 = new Pose(1, 0, 0, 1);
      Pose pose2 = new Pose(2, 5, 5, 2);
      Pose pose3 = new Pose(3, 10, 10, 3);
      Pose pose4 = new Pose(4, 15, 15, 4);

      fusionSlam.addPose(pose1);
      fusionSlam.addPose(pose2);

      assertEquals(pose1, fusionSlam.getPoseAtTime(1), "Pose at time 1 should match.");
      assertEquals(pose2, fusionSlam.getPoseAtTime(2), "Pose at time 2 should match.");
      assertNotEquals(pose3, pose4, "Pose 3 and 4 should not match.");
      assertNull(fusionSlam.getPoseAtTime(5), "Pose at time 5 should be null.");
   }

   /**
    * Test: Transforms local coordinates to global coordinates.
    * Pre-Condition: Pose is available for transformation.
    * Post-Condition: Global coordinates match expected values.
    * Invariant: Number of points remains the same.
    */
   @Test
   void testGlobalTransformation() {
      Pose pose = new Pose(1, 2, 2, 45);
      ArrayList<CloudPoint> localPoints = new ArrayList<>(Arrays.asList(
            new CloudPoint(1, 1),
            new CloudPoint(2, 2),
            new CloudPoint(3, 3)));

      List<List<Double>> globalPoints = fusionSlam.convertToGlobalCoordinates(localPoints, pose);

      assertNotNull(globalPoints, "Global points should not be null.");
      assertEquals(3, globalPoints.size(), "Global points size should match local points size.");
   }

   /**
    * Test: Processes a new tracked object with multiple points and creates a new
    * landmark.
    * Pre-Condition: Pose exists for the tracked object.
    * Post-Condition: A new landmark with multiple points is added.
    * Invariant: Landmark ID and size are consistent.
    */
   @Test
   void testProcessTrackedObject_NewLandmarkMultiplePoints() {
      Pose pose = new Pose(1, 0, 0, 1);
      fusionSlam.addPose(pose);

      ConcurrentLinkedQueue<TrackedObject> trackedObjects = new ConcurrentLinkedQueue<>();
      TrackedObject trackedObject = new TrackedObject("L1", 1, "Landmark",
            new ArrayList<>(Arrays.asList(
                  new CloudPoint(1, 1),
                  new CloudPoint(2, 2),
                  new CloudPoint(3, 3))));

      trackedObjects.add(trackedObject);
      fusionSlam.analyzeObjects(trackedObjects);

      List<LandMark> landmarks = fusionSlam.getLandmarks();
      assertEquals(1, landmarks.size(), "One landmark should be added.");
      assertEquals("L1", landmarks.get(0).getID(), "Landmark ID should match.");
      assertEquals(3, landmarks.get(0).getPoints().size(), "Landmark should contain multiple points.");
   }

   /**
    * Test: Handles tracked objects without a corresponding pose.
    * Pre-Condition: No pose exists for the tracked object.
    * Post-Condition: No landmarks are added.
    * Invariant: Landmark list remains empty.
    */
   @Test
   void testProcessTrackedObject_NoPose() {
      ArrayList<CloudPoint> coordinates = new ArrayList<>(Arrays.asList(new CloudPoint(1, 1)));
      ConcurrentLinkedQueue<TrackedObject> trackedObjects = new ConcurrentLinkedQueue<>();
      TrackedObject trackedObject = new TrackedObject("L1", 2, "Landmark", coordinates);

      trackedObjects.add(trackedObject);
      fusionSlam.analyzeObjects(trackedObjects);

      List<LandMark> landmarks = fusionSlam.getLandmarks();
      assertTrue(landmarks.isEmpty(), "No landmarks should be added if pose is missing.");
   }
}
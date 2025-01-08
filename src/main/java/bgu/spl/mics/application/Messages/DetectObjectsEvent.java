package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Broadcast {
   private String detectorName;
   private int sentTime;
   private StampedDetectedObjects detectedObjects;

   public DetectObjectsEvent(String detectorName, int sentTime, StampedDetectedObjects detectedObjects) {
      this.detectorName = detectorName;
      this.sentTime = sentTime;
      this.detectedObjects = detectedObjects;
   }

   public String getDetectorName() {
      return detectorName;
   }

   public int getSentTime() {
      return sentTime;
   }

   public StampedDetectedObjects getDetectedObjects() {
      return detectedObjects;
   }
}

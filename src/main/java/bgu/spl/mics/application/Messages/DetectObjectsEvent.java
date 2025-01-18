package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
   private String detectorName;
   private int sentTime;
   private StampedDetectedObjects stampedDetectedObjects;

   public DetectObjectsEvent(String detectorName, int sentTime, StampedDetectedObjects stampedDetectedObjects) {
      this.detectorName = detectorName;
      this.sentTime = sentTime;
      this.stampedDetectedObjects = stampedDetectedObjects;
   }

   public String getDetectorName() {
      return detectorName;
   }

   public int getSentTime() {
      return sentTime;
   }

   public StampedDetectedObjects getStampedDetectedObjects() {
      return stampedDetectedObjects;
   }
}

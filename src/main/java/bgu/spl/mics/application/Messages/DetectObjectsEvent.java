package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
   private String detectorName;
   private int detectedTime;
   private StampedDetectedObjects stampedDetectedObjects;

   public DetectObjectsEvent(String detectorName, int detectedTime, StampedDetectedObjects stampedDetectedObjects) {
      this.detectorName = detectorName;
      this.detectedTime = detectedTime;
      this.stampedDetectedObjects = stampedDetectedObjects;
   }

   public String getDetectorName() {
      return detectorName;
   }

   public int getDetectedTime() {
      return detectedTime;
   }

   public StampedDetectedObjects getStampedDetectedObjects() {
      return stampedDetectedObjects;
   }
}

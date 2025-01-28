package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
   private String detectorName;
   private int detectedTick;
   private StampedDetectedObjects stampedDetectedObjects;
   private Boolean isError;
   private int handledTick;
   private Boolean inserted;

   public DetectObjectsEvent(String detectorName, int detectedTick, int handledTick, StampedDetectedObjects stampedDetectedObjects, Boolean isError, Boolean inserted) {
      this.detectorName = detectorName;
      this.detectedTick = detectedTick;
      this.stampedDetectedObjects = stampedDetectedObjects;
      this.isError = isError;
      this.handledTick = handledTick;
      this.inserted = false;
   }

   public String getDetectorName() {
      return detectorName;
   }

    public int getDetectedTick() {
        return detectedTick;
    }

    public int getHandledTick() {
        return handledTick;
    }

   public StampedDetectedObjects getStampedDetectedObjects() {
      return stampedDetectedObjects;
   }

    public Boolean getIsError() {
        return isError;
    }

    public Boolean getInserted() {
        return inserted;
    }
    public void setInserted(Boolean inserted) {
        this.inserted = inserted;
    }
}

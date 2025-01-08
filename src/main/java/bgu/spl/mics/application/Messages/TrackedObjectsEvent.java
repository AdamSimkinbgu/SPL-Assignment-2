package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;

public class TrackedObjectsEvent implements Event<Boolean> {
   private String errorMsg;
   private String senderName;

   public TrackedObjectsEvent(String errorMsg, String senderName) {
      this.errorMsg = errorMsg;
      this.senderName = senderName;
   }

   public String getErrorMsg() {
      return errorMsg;
   }

   public String getSenderName() {
      return senderName;
   }
}

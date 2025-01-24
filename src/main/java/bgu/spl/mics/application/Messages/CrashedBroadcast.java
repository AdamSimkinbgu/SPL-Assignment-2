package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
   private String errorMsg;
   private String Crasher; // another great name by adam, i know
   private int timeCrashed;

   public CrashedBroadcast(String errorMsg, String Crasher, int timeCrashed) {
      this.errorMsg = errorMsg;
      this.Crasher = Crasher;
      this.timeCrashed = timeCrashed;
   }

   public String getErrorMsg() {
      return errorMsg;
   }

   public String getCrasher() {
      return Crasher;
   }

   public int getTimeCrashed() {
      return timeCrashed;
   }
}

package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
   private String errorMsg;
   private String Crasher; // another great name by adam, i know

   public CrashedBroadcast(String errorMsg, String Crasher) {
      this.errorMsg = errorMsg;
      this.Crasher = Crasher;
   }

   public String getErrorMsg() {
      return errorMsg;
   }

   public String getCrasher() {
      return Crasher;
   }
}

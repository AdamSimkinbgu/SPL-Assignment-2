package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast {
   private String terminatorName; // had to call it terminator because the name is so cool!

   public TerminatedBroadcast(String terminatorName) {
      this.terminatorName = terminatorName;
   }

   public String getTerminatorName() {
      return terminatorName;
   }
}

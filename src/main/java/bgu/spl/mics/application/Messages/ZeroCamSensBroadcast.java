package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Broadcast;

public class ZeroCamSensBroadcast implements Broadcast {
   private int activeSensors;
   private int activeCameras;

   public ZeroCamSensBroadcast(int activeSensors, int activeCameras) {
      this.activeSensors = activeSensors;
      this.activeCameras = activeCameras;
   }

   public int getActiveSensors() {
      return activeSensors;
   }

   public int getActiveCameras() {
      return activeCameras;
   }
}

package bgu.spl.mics.application.Messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Boolean> {
   private String sender;
   private Pose pose;

   public PoseEvent(String sender, Pose pose) {
      this.sender = sender;
      this.pose = pose;
   }

   public String getSender() {
      return sender;
   }

   public Pose getPose() {
      return pose;
   }
}

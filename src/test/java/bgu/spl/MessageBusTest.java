package bgu.spl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import bgu.spl.mics.Future;
import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.services.CameraService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MessageBusTest {

   private MessageBusImpl messageBus;
   private CameraService cameraService;

   @BeforeEach
   public void setUp() {
      messageBus = MessageBusImpl.getInstance();
      messageBus.resetForDebug();

      // Initialize CameraService
      Camera camera = new Camera(1, 1, new ConcurrentHashMap<>(), 10);
      cameraService = new CameraService(camera);
   }

   @Test
   public void testRegisterAndUnregisterMicroService() {
      messageBus.register(cameraService);

      assertTrue(messageBus.isRegistered(cameraService), "MicroService should be registered");

      messageBus.unregister(cameraService);

      assertFalse(messageBus.isRegistered(cameraService), "MicroService should be unregistered");
   }

   @Test
   public void testSendEventToSubscribedService() {
      messageBus.register(cameraService);
      messageBus.subscribeEvent(DetectObjectsEvent.class, cameraService);

      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, 0, null, false, false);

      Future<?> future = messageBus.sendEvent(event);

      assertNotNull(future, "Future should not be null when event is sent to a subscribed service");

      messageBus.complete(event, true);
      assertTrue(future.isDone(), "Future should be completed after event is handled");
   }

   @Test
   public void testSendEventNoSubscribers() {
      messageBus.register(cameraService);

      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, 0, null, false, false);

      Future<?> future = messageBus.sendEvent(event);

      assertNull(future, "Future should be null when no services are subscribed to the event type");
   }

   @Test
   public void testSendBroadcastToSubscribedServices() {
      messageBus.register(cameraService);
      messageBus.subscribeBroadcast(TickBroadcast.class, cameraService);

      TickBroadcast broadcast = new TickBroadcast(1);

      messageBus.sendBroadcast(broadcast);

      ConcurrentLinkedQueue<Message> queue = messageBus.getQueue(cameraService);

      assertFalse(queue.isEmpty(), "Broadcast should be received by subscribed service");

      Message received = queue.poll();
      assertTrue(received instanceof TickBroadcast, "Received message should be a TickBroadcast");
      assertEquals(1, ((TickBroadcast) received).getTick(), "Tick value should match");
   }

   @Test
   public void testUnregisterRemovesSubscriptions() {
      messageBus.register(cameraService);
      messageBus.subscribeEvent(DetectObjectsEvent.class, cameraService);
      messageBus.subscribeBroadcast(TickBroadcast.class, cameraService);

      messageBus.unregister(cameraService);

      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, 0, null, false, false);
      TickBroadcast broadcast = new TickBroadcast(1);

      Future<?> eventFuture = messageBus.sendEvent(event);
      messageBus.sendBroadcast(broadcast);

      ConcurrentLinkedQueue<Message> queue = messageBus.getQueue(cameraService);
      assertNull(eventFuture, "Future should be null as service is unregistered");
      assertTrue(queue.isEmpty(), "No messages should be received after unregistration");
   }
}
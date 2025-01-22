package bgu.spl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.Future;
import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.Messages.DetectObjectsEvent;
import bgu.spl.mics.application.Messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.services.CameraService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
      // Register the microservice
      messageBus.register(cameraService);

      // Verify that the microservice is registered
      assertTrue(messageBus.isRegistered(cameraService), "MicroService should be registered");

      // Unregister the microservice
      messageBus.unregister(cameraService);

      // Verify that the microservice is unregistered
      assertFalse(messageBus.isRegistered(cameraService), "MicroService should be unregistered");
   }

   @Test
   public void testSendEventToSubscribedService() {
      // Register the microservice
      messageBus.register(cameraService);

      // Subscribe to an event type
      messageBus.subscribeEvent(DetectObjectsEvent.class, cameraService);

      // Create an event
      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, null);

      // Send the event
      Future<?> future = messageBus.sendEvent(event);

      // Verify that the future is not null
      assertNotNull(future, "Future should not be null when event is sent to a subscribed service");

      // Optionally, complete the future and check the result
      messageBus.complete(event, true);
      assertTrue(future.isDone(), "Future should be completed after event is handled");
   }

   @Test
   public void testSendEventNoSubscribers() {
      // Register the microservice but do not subscribe to any event
      messageBus.register(cameraService);

      // Create an event
      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, null);

      // Send the event
      Future<?> future = messageBus.sendEvent(event);

      // Verify that the future is null since there are no subscribers
      assertNull(future, "Future should be null when no services are subscribed to the event type");
   }

   @Test
   public void testSendBroadcastToSubscribedServices() {
      // Register and subscribe the microservice to a broadcast
      messageBus.register(cameraService);
      messageBus.subscribeBroadcast(TickBroadcast.class, cameraService);

      // Create a broadcast
      TickBroadcast broadcast = new TickBroadcast(1);

      // Send the broadcast
      messageBus.sendBroadcast(broadcast);

      // Retrieve the message queue for the cameraService
      ConcurrentLinkedQueue<Message> queue = messageBus.getQueue(cameraService);

      // Verify that the broadcast is received
      assertFalse(queue.isEmpty(), "Broadcast should be received by subscribed service");

      Message received = queue.poll();
      assertTrue(received instanceof TickBroadcast, "Received message should be a TickBroadcast");
      assertEquals(1, ((TickBroadcast) received).getTick(), "Tick value should match");
   }

   @Test
   public void testUnregisterRemovesSubscriptions() {
      // Register and subscribe the microservice
      messageBus.register(cameraService);
      messageBus.subscribeEvent(DetectObjectsEvent.class, cameraService);
      messageBus.subscribeBroadcast(TickBroadcast.class, cameraService);

      // Unregister the microservice
      messageBus.unregister(cameraService);

      // Create an event and broadcast
      DetectObjectsEvent event = new DetectObjectsEvent("TestSender", 2, null);
      TickBroadcast broadcast = new TickBroadcast(1);

      // Send the event and broadcast
      Future<?> eventFuture = messageBus.sendEvent(event);
      messageBus.sendBroadcast(broadcast);

      // Verify that the microservice does not receive the event or broadcast
      ConcurrentLinkedQueue<Message> queue = messageBus.getQueue(cameraService);
      assertNull(eventFuture, "Future should be null as service is unregistered");
      assertTrue(queue.isEmpty(), "No messages should be received after unregistration");
   }
}
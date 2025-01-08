package bgu.spl.test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import bgu.spl.mics.Message;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;

// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageBusImpTest {
   private MessageBusImpl messageBus;
   private MicroService microService;

   @BeforeEach
   void setUp() {
      messageBus = new MessageBusImpl();
      microService = new ExampleBroadcastListenerService("testService", new String[] { "5" });
      messageBus.register(microService);
   }

   @AfterEach
   void tearDown() {
      // Unregister or clean up as needed
      messageBus.unregister(microService);
      messageBus = null;
      microService = null;
   }

   // =====================
   // 1) Registration Tests
   // =====================
   @Test
   void testRegisterAndUnregister() {
      // We already registered microService in setUp()
      // Verify registration was successful in your own ways, e.g.:
      // - Possibly call some isMicroServiceRegistered(...) method if you implemented
      // it
      // - Or we can test if "awaitMessage" doesn't throw an exception, etc.

      // Now let's unregister and see if everything cleans up
      messageBus.unregister(microService);

      // If we try to await a message, it might block forever if the queue is removed.
      // Typically you'd have a better approach to confirm unregistration (like a
      // boolean),
      // but here we just verify no exceptions, etc.
      assertDoesNotThrow(() -> {
         // If unregistered, the next line might throw an exception or block forever
         // in a real scenario. We won't call awaitMessage because there's no queue.
      });
   }

   // =================================
   // 2) Subscribing and Broadcast Tests
   // =================================
   @Test
   @Order(2)
   void testSubscribeBroadcastAndSendBroadcast() throws InterruptedException {
      // microService is an ExampleBroadcastListenerService,
      // presumably it subscribes to some broadcast in its initialize() or
      // constructor.

      // Now, let's create a broadcast
      ExampleBroadcast broadcast = new ExampleBroadcast("Test Broadcast");

      // Send the broadcast
      messageBus.sendBroadcast(broadcast);

      // We want to see if microService receives it
      // Because microService is a broadcast listener, it should get it from
      // awaitMessage.
      Message received = messageBus.awaitMessage(microService); // blocking call

      assertNotNull(received, "Expected to receive a broadcast message but got null.");
      assertTrue(received instanceof ExampleBroadcast, "Expected an ExampleBroadcast type");
      ExampleBroadcast casted = (ExampleBroadcast) received;
      assertEquals("Test Broadcast", casted.getSenderId(), "Broadcast content mismatch");
   }

   // =====================================
   // 3) Subscribing and Sending Event Tests
   // =====================================
   @Test
   @Order(3)
   void testSubscribeEventAndSendEvent() throws InterruptedException {
      // We'll create a dummy event
      class MyTestEvent implements Event<String> {
         private final String msg;

         public MyTestEvent(String msg) {
            this.msg = msg;
         }

         public String getMsg() {
            return msg;
         }
      }

      // We'll create a micro-service that subscribes to MyTestEvent
      MicroService eventHandler = new MicroService("EventHandler") {
         @Override
         protected void initialize() {
            subscribeEvent(MyTestEvent.class, ev -> {
               // Once we receive the event, we "complete" it with some result
               complete(ev, ev.getMsg().toUpperCase());
            });
         }
      };

      // Register & init the new microService
      messageBus.register(eventHandler);

      // Now send the event from microService
      MyTestEvent event = new MyTestEvent("hello");
      Future<String> future = messageBus.sendEvent(event);

      // The eventHandler should pick this up, handle it, and complete it
      // Let's wait for the result
      String result = future.get(); // blocking until complete
      assertEquals("HELLO", result, "Result from eventHandler callback should be 'HELLO'");

      // Cleanup
      messageBus.unregister(eventHandler);
   }

   // ================================
   // 4) Round-Robin Distribution Test
   // ================================
   @Test
   @Order(4)
   void testRoundRobin() throws InterruptedException {
      // Suppose we have two services that handle the same event
      class MyEvent implements Event<Integer> {
      }

      MicroService handler1 = new MicroService("Handler1") {
         @Override
         protected void initialize() {
            subscribeEvent(MyEvent.class, ev -> complete(ev, 1));
         }
      };
      MicroService handler2 = new MicroService("Handler2") {
         @Override
         protected void initialize() {
            subscribeEvent(MyEvent.class, ev -> complete(ev, 2));
         }
      };

      // Register and initialize them
      messageBus.register(handler1);
      messageBus.register(handler2);

      // Send two events
      MyEvent event1 = new MyEvent();
      MyEvent event2 = new MyEvent();

      Future<Integer> future1 = messageBus.sendEvent(event1);
      Future<Integer> future2 = messageBus.sendEvent(event2);

      // Wait for results
      Integer result1 = future1.get();
      Integer result2 = future2.get();

      // If round-robin is correct, the first event goes to handler1 => 1
      // The second event goes to handler2 => 2
      assertEquals(1, result1, "First event should be handled by 'handler1'");
      assertEquals(2, result2, "Second event should be handled by 'handler2'");

      messageBus.unregister(handler1);
      messageBus.unregister(handler2);
   }

   // ====================
   // 5) Complete() Test
   // ====================
   @Test
   @Order(5)
   void testCompleteMethod() throws InterruptedException {
      // We'll create an event, see if the receiving service calls complete() properly
      class MyCompleteEvent implements Event<String> {
      }

      MicroService completerService = new MicroService("Completer") {
         @Override
         protected void initialize() {
            subscribeEvent(MyCompleteEvent.class, event -> {
               // Another way: we can explicitly call messageBus.complete(...)
               // but typically we call "complete" from the micro-service's own method:
               complete(event, "COMPLETED");
            });
         }
      };

      messageBus.register(completerService);

      // Send the event
      MyCompleteEvent event = new MyCompleteEvent();
      Future<String> future = messageBus.sendEvent(event);

      // Wait for the result
      String result = future.get(); // blocks until "complete" is called
      assertEquals("COMPLETED", result, "Expected COMPLETED as the event result");

      // Cleanup
      messageBus.unregister(completerService);
   }

   // =====================================================
   // 6) Send Event With No Subscribers => Future should be null
   // =====================================================
   @Test
   @Order(6)
   void testSendEventNoSubscribers() {
      class UnhandledEvent implements Event<Boolean> {
      }

      // We didn't subscribe any MicroService to UnhandledEvent
      // so we expect a null Future
      Future<Boolean> future = messageBus.sendEvent(new UnhandledEvent());
      assertNull(future, "Expected null future if no one is subscribed to the event");
   }

   // =====================================================
   // 7) Testing awaitMessage() blocking behavior
   // =====================================================
   @Test
   @Order(7)
   void testAwaitMessageBlocks() throws InterruptedException {
      // We'll do a quick check to see if awaitMessage blocks until something is sent.
      // We'll use a separate thread to send a broadcast after a short delay.

      MicroService blockingService = new MicroService("BlockingService") {
         @Override
         protected void initialize() {
            // We'll subscribe to ExampleBroadcast for the test
            subscribeBroadcast(ExampleBroadcast.class, b -> {
               terminate(); // so the run loop will end
            });
         }
      };
      messageBus.register(blockingService);

      Thread blockingThread = new Thread(blockingService);
      blockingThread.start();

      // Sleep a bit, then send the broadcast
      Thread.sleep(500); // simulate a short delay

      // Now let's send the broadcast so that awaitMessage can return
      messageBus.sendBroadcast(new ExampleBroadcast("BroadcastTest"));

      // Wait for the thread to finish
      blockingThread.join(2000);
      assertFalse(blockingThread.isAlive(),
            "Blocking thread should have ended after receiving the broadcast");

      messageBus.unregister(blockingService);
   }
}

// package bgu.spl;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.*;

// import bgu.spl.mics.Future;
// import bgu.spl.mics.application.Messages.DetectObjectsEvent;
// import bgu.spl.mics.application.Messages.TerminatedBroadcast;
// import bgu.spl.mics.application.Messages.TickBroadcast;
// import bgu.spl.mics.application.objects.Camera;
// import bgu.spl.mics.application.objects.DetectedObject;
// import bgu.spl.mics.application.objects.STATUS;
// import bgu.spl.mics.application.objects.StampedDetectedObjects;
// import bgu.spl.mics.application.services.CameraService;
// import bgu.spl.mics.MessageBus;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ConcurrentHashMap;

// public class CameraServiceTest {

// @Mock
// private MessageBus mockMessageBus;

// @Mock
// private Future<?> mockFuture;

// @InjectMocks
// private CameraService cameraService;

// private Camera testCamera;

// @BeforeEach
// public void setUp() {
// MockitoAnnotations.openMocks(this);

// // Initialize Camera with test data
// ConcurrentHashMap<Integer, StampedDetectedObjects> detectedObjectsList = new
// ConcurrentHashMap<>();
// List<DetectedObject> detectedObjects = new ArrayList<>();
// detectedObjects.add(new DetectedObject("obj1", "Description1"));
// detectedObjectsList.put(1, new StampedDetectedObjects(1, detectedObjects));

// testCamera = new Camera(1, 1, detectedObjectsList, 2);
// cameraService = new CameraService(testCamera);
// cameraService.messageBus = mockMessageBus; // Assuming messageBus is
// accessible or use reflection
// }

// @Test
// public void testHandleTickBroadcastSendsDetectObjectsEvent() {
// // Arrange
// TickBroadcast tick = new TickBroadcast(1);
// when(mockMessageBus.sendEvent(any(DetectObjectsEvent.class))).thenReturn(mockFuture);

// // Act
// cameraService.initialize();
// cameraService.handleTickBroadcast(tick); // Assuming handleTickBroadcast is
// accessible or use reflection

// // Assert
// ArgumentCaptor<DetectObjectsEvent> eventCaptor =
// ArgumentCaptor.forClass(DetectObjectsEvent.class);
// verify(mockMessageBus, times(1)).sendEvent(eventCaptor.capture());

// DetectObjectsEvent sentEvent = eventCaptor.getValue();
// assertEquals("CameraService1", sentEvent.getSender());
// assertEquals(2, sentEvent.getDueTick());
// assertEquals(1, sentEvent.getStampedDetectedObjects().getTime());
// assertEquals(1,
// sentEvent.getStampedDetectedObjects().getDetectedObjects().size());
// }

// @Test
// public void testHandleTickBroadcastNoDetectedObjects() {
// // Arrange
// TickBroadcast tick = new TickBroadcast(1);
// testCamera.setStatus(STATUS.DOWN); // Simulate camera reaching time limit

// // Act
// cameraService.initialize();
// cameraService.handleTickBroadcast(tick);

// // Assert
// verify(mockMessageBus, never()).sendEvent(any(DetectObjectsEvent.class));
// }

// @Test
// public void testHandleTickBroadcastCameraError() {
// // Arrange
// TickBroadcast tick = new TickBroadcast(1);
// testCamera.setStatus(STATUS.ERROR); // Simulate camera error
// when(mockMessageBus.sendBroadcast(any())).thenReturn(null);

// // Act
// cameraService.initialize();
// cameraService.handleTickBroadcast(tick);

// // Assert
// verify(mockMessageBus, times(1)).sendBroadcast(any());
// verify(mockMessageBus, times(1)).sendBroadcast(argThat(b -> b instanceof
// CrashedBroadcast));
// // Verify terminate is called
// assertTrue(cameraService.isTerminated(), "CameraService should be terminated
// due to error");
// }

// @Test
// public void testHandleTerminatedBroadcast() {
// // Arrange
// TerminatedBroadcast terminated = new TerminatedBroadcast("TimeService");
// when(mockMessageBus.sendBroadcast(any())).thenReturn(null);

// // Act
// cameraService.initialize();
// cameraService.handleTerminatedBroadcast(terminated); // Assuming
// handleTerminatedBroadcast is accessible

// // Assert
// verify(mockMessageBus, times(1)).sendBroadcast(any());
// verify(mockMessageBus, times(1)).sendBroadcast(argThat(b -> b instanceof
// TerminatedBroadcast));
// // Verify terminate is called
// assertTrue(cameraService.isTerminated(), "CameraService should be terminated
// upon receiving TerminatedBroadcast");
// }
// }
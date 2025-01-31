package bgu.spl.mics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

import bgu.spl.mics.application.services.CameraService;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus
 * interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for
 * unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	private static class SingletonHolder {
		private static MessageBusImpl instance = new MessageBusImpl();
	}

	private final ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> microhashmap;
	private final ConcurrentHashMap<Class<? extends Event<?>>, ConcurrentLinkedQueue<MicroService>> eventshashmap;
	private final ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadcasthashmap;
	private final ConcurrentHashMap<Event<?>, Future<?>> futurehashmap;

	public static MessageBusImpl getInstance() {
		return SingletonHolder.instance;
	}

	private MessageBusImpl() {
		this.microhashmap = new ConcurrentHashMap<>();
		this.eventshashmap = new ConcurrentHashMap<>();
		this.broadcasthashmap = new ConcurrentHashMap<>();
		this.futurehashmap = new ConcurrentHashMap<>();
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		eventshashmap.putIfAbsent(type, new ConcurrentLinkedQueue<>());
		ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(type);
		synchronized (eventQueue) {
			if (!eventQueue.contains(m)) {
				eventQueue.add(m);
				System.out.println("[EVENT SUBSCRIBED] - " + Thread.currentThread().getName() + ": (MicroService: "
						+ m.getName() + ") " + "subscribing to event of type " + type);
			}
		}
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		broadcasthashmap.putIfAbsent(type, new ConcurrentLinkedQueue<>());
		ConcurrentLinkedQueue<MicroService> broadcastQueue = broadcasthashmap.get(type);
		synchronized (broadcastQueue) {
			if (!broadcastQueue.contains(m)) {
				broadcastQueue.add(m);
				System.out.println("[BROADCAST SUBSCRIBED] - " + Thread.currentThread().getName() + ": (MicroService: "
						+ m.getName() + ") " + "subscribing to broadcast of type " + type);
			} else
				System.out.println("[SUBSCRIBE BROADCAST ERROR] - " + "MicroService " + m.getName()
						+ " already subscribed to Broadcast " + type);
		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		synchronized (e) { // this is shit
			Future<T> future = (Future<T>) futurehashmap.remove(e);
			if (future != null)
				future.resolve(result);
		}
		System.out.println("[FUTURE COMPLETED] - " + "Event " + e.getClass() + " completed with result " + result);

	}

	@Override
	public void sendBroadcast(Broadcast b) {
		ConcurrentLinkedQueue<MicroService> broadcastQueue = broadcasthashmap.get(b.getClass());
		String bClass = broadcastQueue.getClass().toString();
		System.out.println("{BROADCASTQUEUE PRINT}: " + bClass + " queue is: " + broadcastQueue);
		if (broadcastQueue != null) {
			synchronized (broadcastQueue) {
				for (MicroService subscribed : broadcastQueue) {

					ConcurrentLinkedQueue<Message> messageQueue = microhashmap.get(subscribed);
					if (messageQueue != null) {
						synchronized (messageQueue) {
							messageQueue.add(b);
							messageQueue.notifyAll(); // Notify the microservice thread
							System.out.println(
									"[SENDBROADCAST] - " + "Broadcast " + b.getClass() + "sent to " + subscribed.getName());
						}
						// } else {
						// System.err.println("[SENDBROADCAST ERROR] - " + "MicroService " +
						// subscribed.getName()
						// + " not found in microhashmap");
					}
				}
			}
		} else {
			System.out
					.println("[SENDBROADCAST ERROR] - " + "Broadcast " + b.getClass() + " not found in broadcasthashmap");
		}
	}

	// @Override
	// public void sendBroadcast(Broadcast b) {
	// if (broadcasthashmap.containsKey(b.getClass())) {
	// ConcurrentLinkedQueue<MicroService> broadcastQueue =
	// broadcasthashmap.get(b.getClass());
	// if (broadcastQueue != null) {
	// synchronized (broadcastQueue) {
	// if (!broadcastQueue.isEmpty()) {
	// for (MicroService subscribed : broadcastQueue) {
	// ConcurrentLinkedQueue<Message> messageQueue = microhashmap.get(subscribed);
	// if (messageQueue != null) {
	// synchronized (messageQueue) {
	// if (messageQueue != null) {
	// messageQueue.add(b);
	// messageQueue.notify();
	// System.out.println("[SENDBROADCAST] - " + "Broadcast " + b.getClass() + "
	// sent to "
	// + subscribed.getName());
	// return;
	// } // else {
	// // System.err.println("[SENDBROADCAST ERROR] - " + "MicroService " +
	// // subscribed.getName() + " not found in microhashmap");
	// // }
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	// System.out.println("[SENDBROADCAST ERROR] - " + "Broadcast " + b.getClass() +
	// " not found in broadcasthashmap");
	// }

	// @Override
	// public <T> Future<T> sendEvent(Event<T> e) {
	// MicroService chosen = null;
	// if (eventshashmap.containsKey(e.getClass())) {
	// ConcurrentLinkedQueue<MicroService> eventQueue =
	// eventshashmap.get(e.getClass());
	// if (eventQueue != null && !eventQueue.isEmpty()) {
	// synchronized (eventQueue) {
	// if (!eventQueue.isEmpty()) {
	// chosen = eventQueue.poll();
	// eventQueue.add(chosen);
	// }
	// }
	// Future<T> future = new Future<>();
	// futurehashmap.put(e, future);
	// if (chosen != null) {
	// ConcurrentLinkedQueue<Message> messageQueue = microhashmap.get(chosen);
	// if (messageQueue != null) {
	// synchronized (messageQueue) {
	// if (messageQueue != null) {
	// messageQueue.add(e);
	// messageQueue.notify();
	// System.out.println("[SENDEVENT] - " + "Event " + e.getClass() + " sent to " +
	// chosen.getName());
	// } // else {
	// // System.err.println("[SENDEVENT ERROR] - " + "MicroService " +
	// // chosen.getName() + " not found in microhashmap");
	// // futurehashmap.remove(e);
	// // return null;
	// // }
	// }
	// }
	// }
	// return future;
	// }
	// }
	// System.out.println("[SENDEVENT ERROR] - " + "Event " + e.getClass() + " not
	// found in eventshashmap");
	// complete(e, null); // complete the event with null if no microservice is
	// available
	// return null;
	// }

	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		MicroService chosen = null;
		ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(e.getClass());
		if (eventQueue == null) {
			System.out.println("[SENDEVENT ERROR] - " + "Event " + e.getClass() + " not found in eventshashmap");
			return null;
		}
		synchronized (eventQueue) {
			// if (eventQueue == null || eventQueue.isEmpty()) {
			// System.out.println("[SENDEVENT ERROR] - " + "Event " + e.getClass() + " not
			// found in eventshashmap");
			// return null;
			// }

			// if (eventQueue.isEmpty()) {
			// System.out.println("[SENDEVENT ERROR] - " + "Event " + e.getClass() + " Queue
			// is empty");
			// return null;
			// }

			// Round-robin selection of microservice
			while (!eventQueue.isEmpty()) {
				chosen = eventQueue.poll();
				if (chosen != null && microhashmap.containsKey(chosen)) {
					eventQueue.add(chosen); // Re-add to the end for round-robin
					break;
				}
			}

			if (chosen == null) {
				System.out.println("[SENDEVENT ERROR] - " + "No available MicroService for Event " + e.getClass());
				return null;
			}
		}

		// Create and store the Future
		Future<T> future = new Future<>();
		futurehashmap.put(e, future);

		// Retrieve the message queue of the chosen microservice
		ConcurrentLinkedQueue<Message> messageQueue = microhashmap.get(chosen);
		synchronized (messageQueue) {
			// if (messageQueue != null) {
			messageQueue.add(e);
			messageQueue.notify(); // Notify the microservice that a new message has arrived
			System.out.println("[SENDEVENT] - " + "Event " + e.getClass() + " sent to " +
					chosen.getName());

			// } else {
			// System.err.println("[SENDEVENT ERROR] - " + "MicroService " +
			// chosen.getName() + " not found in microhashmap");
			// futurehashmap.remove(e); // Clean up the future as the message won't be
			// processed
			// return null;
			// }

		}
		return future; // Return the Future to allow the sender to wait for the result
	}

	@Override
	public void register(MicroService m) {
		this.microhashmap.putIfAbsent(m, new ConcurrentLinkedQueue<Message>());
		System.out.println("[REGISTERED] - " + Thread.currentThread().getName() + ": (MicroService: " + m.getName() + ") "
				+ " registered");
	}

	// @Override
	// public void unregister(MicroService m) {
	// for (Message message : microhashmap.get(m)) {
	// if (message instanceof Event) {
	// ConcurrentLinkedQueue<MicroService> eventQueue =
	// eventshashmap.get(message.getClass());
	// synchronized (eventQueue) {
	// eventQueue.remove(m);
	// }
	// } else if (message instanceof Broadcast) {
	// ConcurrentLinkedQueue<MicroService> broadcastQueue =
	// broadcasthashmap.get(message.getClass());
	// synchronized (broadcastQueue) {
	// broadcastQueue.remove(m);
	// }
	// }

	// }
	// this.microhashmap.remove(m);
	// System.out.println("MicroService " + m.getName() + " unregistered");
	// }

	@Override
	public void unregister(MicroService m) {
		ConcurrentLinkedQueue<Message> messageQueue = microhashmap.remove(m);
		if (messageQueue != null) {
			// Remove from all event subscriptions
			for (ConcurrentLinkedQueue<MicroService> queue : eventshashmap.values()) {
				synchronized (queue) {
					queue.remove(m);
				}
			}
			// Remove from all broadcast subscriptions
			for (ConcurrentLinkedQueue<MicroService> queue : broadcasthashmap.values()) {
				synchronized (queue) {
					queue.remove(m);
				}
			}
			// Notify any threads waiting on the message queue
			synchronized (messageQueue) {
				messageQueue.notifyAll();
			}
			System.out.println("[UNREGISTERED] - " + Thread.currentThread().getName() + ": (MicroService: " + m.getName()
					+ ") " + " terminated");
		} else {
			System.err.println("[UNREGISTER ERROR] - " + "Error: MicroService " + m.getName() + " was not registered");
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		ConcurrentLinkedQueue<Message> messageQueue = microhashmap.get(m);
		if (messageQueue == null) {
			System.err.println("[AWAITMESSAGE ERROR] - " + "Error: MicroService " + m.getName() + " was not registered");
			return null; // Return null if the microservice is not registered
		}

		synchronized (messageQueue) {
			while (messageQueue.isEmpty()) {
				try {
					messageQueue.wait(); // Wait for new messages
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt(); // Restore interrupted status
					throw e; // Propagate the exception
				}
			}
			return messageQueue.poll();
		}
	}

	public boolean registrationsForTickBroadcastPlease(MicroService microService) {
		return microhashmap.containsKey(microService);
	}

	public void resetForDebug() {
		microhashmap.clear();
		eventshashmap.clear();
		broadcasthashmap.clear();
		futurehashmap.clear();
	}

	public ConcurrentLinkedQueue<Message> getQueue(CameraService cameraService) {
		ConcurrentLinkedQueue<Message> queue = microhashmap.get(cameraService);
		if (queue == null) {
			System.err.println(
					"[GETQUEUE ERROR] - " + "Error: MicroService " + cameraService.getName() + " was not registered");
			queue = new ConcurrentLinkedQueue<>();
		}
		return queue;
	}

	public boolean isRegistered(MicroService service) {
		return microhashmap.containsKey(service);
	}
}

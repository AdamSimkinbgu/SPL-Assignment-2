package bgu.spl.mics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
				System.out.println("MicroService " + m.getName() + " subscribed to Event " + type);
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
				System.out.println("MicroService " + m.getName() + " subscribed to Broadcast " + type);
			} else
				System.out.println("Error: MicroService " + m.getName() + " already subscribed to Broadcast " + type);
		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> future = (Future<T>) futurehashmap.remove(e);
		if (future != null) {
			future.resolve(result);
		} else
			System.out.println("Error: Future not found");
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		ConcurrentLinkedQueue<MicroService> broadcastQueue = broadcasthashmap.get(b.getClass());
		if (broadcastQueue != null) {
			if (!(broadcastQueue.isEmpty())) {
				synchronized (broadcastQueue) {
					for (MicroService subscribedforb : broadcastQueue) {
						if (microhashmap.get(subscribedforb) == null) {
							System.out.println("Error: MicroService " + subscribedforb.getName() + " not found");
						} else {
							microhashmap.get(subscribedforb).add(b);
						}
					}
				}
			} else {
				System.out.println("Error: Broadcast " + b.getClass() + " Queue is empty");
			}
		} else {
			System.out.println("Error: Broadcast " + b.getClass() + " not found");
		}
	}

	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		MicroService chosen = null;
		ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(e.getClass());
		synchronized (eventQueue) {
			if (eventQueue != null) {
				if (!eventQueue.isEmpty()) {
					do {
						chosen = eventQueue.poll();
					} while (!(chosen != null && microhashmap.containsKey(chosen)));
					eventQueue.add(chosen);
				} else {
					System.out.println("Error: Event " + e.getClass() + " has no subscribers");
					return null;
				}
			} else {
				System.out.println("Error: Event " + e.getClass() + " has no queue");
				return null;
			}
		}
		Future<T> future = new Future<>();
		futurehashmap.put(e, future);
		microhashmap.get(chosen).add(e);
		return future;
	}

	@Override
	public void register(MicroService m) {
		this.microhashmap.putIfAbsent(m, new ConcurrentLinkedQueue<Message>());
		System.out.println("MicroService " + m.getName() + " registered");
	}

	@Override
	public void unregister(MicroService m) {
		for (Message message : microhashmap.get(m)) {
			if (message instanceof Event) {
				ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(message.getClass());
				synchronized (eventQueue) {
					eventQueue.remove(m);
				}
			} else if (message instanceof Broadcast) {
				ConcurrentLinkedQueue<MicroService> broadcastQueue = broadcasthashmap.get(message.getClass());
				synchronized (broadcastQueue) {
					broadcastQueue.remove(m);
				}
			}

		}
		this.microhashmap.remove(m);
		System.out.println("MicroService " + m.getName() + " unregistered");
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		while (microhashmap.get(m).isEmpty()) {
			try {
				Thread.currentThread().wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return microhashmap.get(m).poll();

	}
}

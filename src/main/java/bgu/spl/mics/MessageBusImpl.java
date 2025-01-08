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

	private ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> microhashmap;
	private ConcurrentHashMap<Class<? extends Event<?>>, ConcurrentLinkedQueue<MicroService>> eventshashmap;
	private ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadcasthashmap;
	private ConcurrentHashMap<Event<?>, Future<?>> futurehashmap;
	private final Object eventLock, broadcastLock;

	public static MessageBusImpl getInstance() {
		return SingletonHolder.instance;
	}

	private MessageBusImpl() {
		this.microhashmap = new ConcurrentHashMap<>();
		this.eventshashmap = new ConcurrentHashMap<>();
		this.broadcasthashmap = new ConcurrentHashMap<>();
		this.futurehashmap = new ConcurrentHashMap<>();
		this.eventLock = new Object();
		this.broadcastLock = new Object();
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		synchronized (eventLock) {
			eventshashmap.putIfAbsent(type, new ConcurrentLinkedQueue<MicroService>());
			eventshashmap.get(type).add(m);
		}
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		synchronized (broadcastLock) {
			broadcasthashmap.putIfAbsent(type, new ConcurrentLinkedQueue<MicroService>());
			broadcasthashmap.get(type).add(m);
		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> future = (Future<T>) futurehashmap.remove(e);
		if (future != null) {
			future.resolve(result);
		} // where do we notify the message bus that the event is done??
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		synchronized (broadcastLock) {
			while (broadcasthashmap.get(b.getClass()) == null) {
				try {
					broadcastLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			broadcasthashmap.get(b.getClass()).forEach(m -> {
				microhashmap.get(m).add(b);
			});
			broadcastLock.notifyAll();
		}
	}

	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		synchronized (eventLock){
			ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(e.getClass());
			if (eventQueue == null || eventQueue.isEmpty()) {
				return null;
			}
			else{
				MicroService m = eventQueue.poll();
				eventQueue.add(m);
				Future<T> future = new Future<>();
				futurehashmap.put(e, future);
				microhashmap.get(m).add(e);
				eventLock.notifyAll();
				return future;
			}
		}
	}


	@Override
	public void register(MicroService m) {
		this.microhashmap.putIfAbsent(m, new ConcurrentLinkedQueue<Message>());
	}

	@Override
	public void unregister(MicroService m) {
		synchronized (this) {
			this.microhashmap.remove(m);
			this.broadcasthashmap.forEach((k, v) -> v.remove(m));
			this.eventshashmap.forEach((k, v) -> v.remove(m));
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		while (microhashmap.get(m).isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return microhashmap.get(m).poll();

	}
}

package bgu.spl.mics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {

	private static MessageBusImpl instance = null;
	private ConcurrentHashMap<MicroService, ConcurrentLinkedQueue<Message>> microhashmap;
	private ConcurrentHashMap<Class<? extends Event<?>>, ConcurrentLinkedQueue<MicroService>> eventshashmap;
	private ConcurrentHashMap<Class<? extends Broadcast>, ConcurrentLinkedQueue<MicroService>> broadcasthashmap;
	private final Object eventLock, broadcastLock;

	public MessageBusImpl getInstance() {
		if(instance == null) {
			instance = new MessageBusImpl();
		}
		return instance;
	}


	private MessageBusImpl() {
		this.microhashmap = new ConcurrentHashMap<>();
		this.eventshashmap = new ConcurrentHashMap<>();
		this.broadcasthashmap = new ConcurrentHashMap<>();
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
		// TODO Auto-generated method stub

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
		synchronized (eventLock) {
			while (eventshashmap.get(e.getClass()) == null || eventshashmap.get(e.getClass()).isEmpty()) {
				try{
					eventLock.wait();
				} catch (InterruptedException ex){
					Thread.currentThread().interrupt();
				}
			}
			ConcurrentLinkedQueue<MicroService> eventQueue = eventshashmap.get(e.getClass());
			MicroService m = eventQueue.poll();
			eventQueue.add(m);
			Future<T> future = new Future<>();
			microhashmap.get(m).add(e);
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

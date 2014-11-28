import java.util.*;
public class SetPriorityQueue<E extends Comparable> extends PriorityQueue {
	
	public SetPriorityQueue(){
		super();
	}

	public SetPriorityQueue(Collection<? extends E> c){
		super(c);
	}

	public SetPriorityQueue(int initialCapacity){
		super(initialCapacity);
	}

	public SetPriorityQueue(int initialCapacity, Comparator<? super E> comparator){
		super(initialCapacity, comparator);
	}

	public SetPriorityQueue(SetPriorityQueue<? extends E> c){
		super(c);
	}

	public SetPriorityQueue(SortedSet<? extends E> c){
		super(c);
	}

	public boolean offer(E e) {
	  if (contains(e)) {
	    return false; 
	  } else {
	    return super.offer(e);
	  }
	}
}
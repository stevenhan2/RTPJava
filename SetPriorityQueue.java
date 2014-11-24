import java.util.*;
public class SetPriorityQueue extends PriorityQueue {
	
	public PriorityQueue(){
		super();
	}

	public PriorityQueue(Collection<? extends E> c){
		super(c);
	}

	public PriorityQueue(int initialCapacity){
		super(initialCapacity);
	}

	public PriorityQueue(int initialCapacity, Comparator<? super E> comparator){
		super(initialCapacity, comparator);
	}

	public PriorityQueue(PriorityQueue<? extends E> c){
		super(c);
	}

	public PriorityQueue(SortedSet<? extends E> c){
		super(c);
	}

	@Override
	public boolean offer(E e) {
	  if (contains(e)) {
	    return false; 
	  } else {
	    super.offer(e);
	  }
	}
}
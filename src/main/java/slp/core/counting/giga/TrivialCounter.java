package slp.core.counting.giga;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrivialCounter {

	int count;
	Map<List<Integer>, Integer> counts;
	
	public TrivialCounter() {
		this.count = 0;
		this.counts = new HashMap<>();
	}

	public int getCount() {
		return this.count;
	}

	public Map<List<Integer>, Integer> getCounts() {
		return this.counts;
	}
	
	public void count(List<Integer> l) {
		this.count++;
		if (l.isEmpty()) return;
		this.counts.merge(l, 1, Integer::sum);
	}
	
}

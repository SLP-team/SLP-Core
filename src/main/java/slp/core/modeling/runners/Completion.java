package slp.core.modeling.runners;

import java.util.List;

import slp.core.util.Pair;

public class Completion {
	private final Integer realIx;
	private final List<Pair<Integer, Double>> completions;
	
	public Completion(Integer realIx, List<Pair<Integer, Double>> predictions) {
		this.realIx = realIx;
		this.completions = predictions;
	}

	public Completion(List<Pair<Integer, Double>> predictions) {
		this.realIx = null;
		this.completions = predictions;
	}

	public List<Pair<Integer, Double>> getPredictions() {
		return completions;
	}

	public Integer getRealIx() {
		return realIx;
	}
	
	public int getRank() {
		if (this.realIx == null) return -1;
		else {
			for (int i = 0; i < this.completions.size(); i++) {
				if (this.completions.get(i).left.equals(this.realIx)) {
					return i;
				}
			}
			return -1;
		}
	}
}

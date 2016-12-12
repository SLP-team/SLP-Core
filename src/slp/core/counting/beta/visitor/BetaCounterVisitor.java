package slp.core.counting.beta.visitor;

import slp.core.counting.beta.BetaCounter;
import slp.core.counting.beta.BetaCounterArray;
import slp.core.counting.beta.BetaCounterMap;
import slp.core.counting.beta.BetaCounterSingle;
import slp.core.counting.beta.BetaCounterSmall;

public interface BetaCounterVisitor {

	public default void visit(BetaCounter counter) {
		if (!(counter instanceof BetaCounterMap)) return;
		preVisit();
		visit((BetaCounterMap) counter);
		postVisit();
	}
	
	public void preVisit();

	public void postVisit();

	public void visit(BetaCounterMap betaCounterMap);

	public void visit(BetaCounterArray betaCounterArray);
	
	public void visit(BetaCounterSmall betaCounterSmall);

	public void visit(BetaCounterSingle betaCounterSingle);
	
}

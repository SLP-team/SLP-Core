package slp.core.counting.beta.visitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import slp.core.counting.beta.BetaCounter;
import slp.core.counting.beta.BetaCounterArray;
import slp.core.counting.beta.BetaCounterMap;
import slp.core.counting.beta.BetaCounterSmall;

public class SimplePrinter {
	public void print(BetaCounter betaCounter, File outputFile) {
		try (FileWriter fw = new FileWriter(outputFile)) {
			Deque<Integer> stack = new ArrayDeque<Integer>();
			printCounter(fw, stack, betaCounter);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printCounter(FileWriter fw, Deque<Integer> stack, BetaCounter betaCounter) {
		if (betaCounter instanceof BetaCounterMap) {
//			BetaCounterMap asMap = (BetaCounterMap) betaCounter;
			// TODO old map code
//			Map<Integer, BetaCounter> successors = asMap.getSuccessors();
//			for (Entry<Integer, BetaCounter> entry : successors.entrySet()) {
//				stack.addFirst(entry.getKey());
//				printCounter(fw, stack, entry.getValue());
//				stack.removeFirst();
//			}
		} else if (betaCounter instanceof BetaCounterArray) {
//			BetaCounterArray asArray = (BetaCounterArray) betaCounter;
			
		} else if (betaCounter instanceof BetaCounterSmall) {
//			BetaCounterSmall asSmall = (BetaCounterSmall) betaCounter;
			
		} else {
//			BetaCounterSingle asSingle = (BetaCounterSingle) betaCounter;
			
		}
	}
}

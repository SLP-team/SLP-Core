package slp.core.counting.beta.visitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import slp.core.counting.beta.BetaCounterArray;
import slp.core.counting.beta.BetaCounterMap;
import slp.core.counting.beta.BetaCounterSingle;
import slp.core.counting.beta.BetaCounterSmall;

public class PrintingVisitor implements BetaCounterVisitor {

	private final File outputFile;
	
	public PrintingVisitor(File outputFile) {
		this.outputFile = outputFile;
	}
	
	private FileWriter fw;
	@Override
	public void preVisit() {
		try {
			this.fw = new FileWriter(this.outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void postVisit() {
		try {
			if (this.fw != null) {
				this.fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void visit(BetaCounterMap betaCounterMap) {
//		Map<Integer, BetaCounter> successors = betaCounterMap.getSuccessors();
		
	}

	@Override
	public void visit(BetaCounterArray betaCounterArray) {
		
	}

	@Override
	public void visit(BetaCounterSmall betaCounterSmall) {
		
	}

	@Override
	public void visit(BetaCounterSingle betaCounterSingle) {
		
	}
	
}

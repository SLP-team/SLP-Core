package slp.core.modeling.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.Model;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramModel;

class Nester {

	private final Model global;
	private final LexerRunner lexerRunner;
	
	private List<ModelRunner> modelRunners;
	private List<File> files;
	private Model mixed;

	public Nester(Model model, LexerRunner lexerRunner, File testRoot, Model testBaseModel) {
		this.global = model;
		this.lexerRunner = lexerRunner;
		this.modelRunners = new ArrayList<>();
		this.files = new ArrayList<>();
		
		this.modelRunners.add(new ModelRunner(this.lexerRunner, testBaseModel));
		this.files.add(testRoot);
		this.mixed = MixModel.standard(this.global, this.modelRunners.get(0).getModel());
	}
	
	public Model getMix() {
		return this.mixed;
	}
	
	public File getTestRoot() {
		return this.files.get(0);
	}

	void updateNesting(File next) {
		List<File> lineage = getLineage(next);
		// If lineage is empty, the current model is the (first meaningful) parent of next and is appropriate
		if (lineage == null || lineage.isEmpty()) return;
		int pos = 1;
		for (; pos < this.files.size(); pos++) {
			if (pos >= lineage.size() || !this.files.get(pos).equals(lineage.get(pos))) {
				this.modelRunners.get(pos - 1).learn(this.files.get(pos));
				this.files.subList(pos, this.files.size()).clear();
				this.modelRunners.subList(pos, this.modelRunners.size()).clear();
				break;
			}
		}
		for (int i = pos; i < lineage.size(); i++) {
			File file = lineage.get(i);
			Model model = NGramModel.standard();
			this.files.add(file);
			this.modelRunners.add(new ModelRunner(this.lexerRunner, model));
			this.modelRunners.get(this.modelRunners.size() - 1).learn(file);
			this.modelRunners.get(this.modelRunners.size() - 2).forget(file);
		}
		this.files.add(next);
		this.modelRunners.get(this.modelRunners.size() - 1).forget(next);
		this.mixed = MixModel.standard(this.global, this.modelRunners.get(0).getModel());
		for (int i = 1; i < this.modelRunners.size(); i++) {
			this.mixed = MixModel.standard(this.mixed, this.modelRunners.get(i).getModel());
		}
		this.mixed.notify(next);
	}

	/**
	 * Returns all non-trivial directories starting from the root file to the new file.
	 * Non-trivial meaning a directory containing more than one regex-matching file or dir itself; 
	 * building a separate nested model for such directories is pointless.
	 * 
	 * @param file The next file to be modeled
	 * @return Path containing all relevant directories from root file inclusive to {@code file} exclusive
	 */
	private List<File> getLineage(File file) {
		List<File> lineage = new ArrayList<>();
		while (!file.getParentFile().equals(this.files.get(0))) {
			if (file.getParentFile().list().length > 1
					&& Arrays.stream(file.getParentFile().listFiles())
						.anyMatch(f -> f.isDirectory() || this.lexerRunner.willLexFile(f))) {
				lineage.add(file.getParentFile());
			}
			file = file.getParentFile();
			if (file.getParentFile() == null) return null;
		}
		lineage.add(this.files.get(0));
		Collections.reverse(lineage);
		return lineage;
	}
}
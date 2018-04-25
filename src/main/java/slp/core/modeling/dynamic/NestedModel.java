package slp.core.modeling.dynamic;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import slp.core.lexing.runners.LexerRunner;
import slp.core.modeling.AbstractModel;
import slp.core.modeling.Model;
import slp.core.modeling.mix.MixModel;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.runners.ModelRunner;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;

public class NestedModel extends AbstractModel {

	private final Model global;
	private final LexerRunner lexerRunner;
	private final Vocabulary vocabulary;
	
	private List<ModelRunner> modelRunners;
	private List<File> files;
	private Model mixed;

	public NestedModel(ModelRunner baseRunner, File testRoot) {
		this(baseRunner.getModel(), baseRunner.getLexerRunner(), baseRunner.getVocabulary(), testRoot);
	}

	public NestedModel(ModelRunner baseRunner, File testRoot, Model testBaseModel) {
		this(baseRunner.getModel(), baseRunner.getLexerRunner(), baseRunner.getVocabulary(), testRoot, testBaseModel);
	}
	
	public NestedModel(Model global, LexerRunner lexerRunner, Vocabulary vocabulary, File testRoot) {
		this(global, lexerRunner, vocabulary, testRoot, null);
	}

	public NestedModel(Model global, LexerRunner lexerRunner, Vocabulary vocabulary, File testRoot, Model testBaseModel) {
		this.global = global;
		this.lexerRunner = lexerRunner;
		this.vocabulary = vocabulary;
		this.modelRunners = new ArrayList<>();
		this.files = new ArrayList<>();
		
		this.modelRunners.add(getBaseRunner(testRoot, testBaseModel));
		this.files.add(testRoot);
		this.mixed = MixModel.standard(this.global, this.modelRunners.get(0).getModel());
	}
	
	private ModelRunner getBaseRunner(File testRoot, Model testBaseModel) {
		ModelRunner baseModelRunner;
		if (testBaseModel == null) {
			testBaseModel = newModel();
			baseModelRunner = newModelRunner(testBaseModel);
			baseModelRunner.learnDirectory(testRoot);
		}
		else {
			baseModelRunner = newModelRunner(testBaseModel);
		}
		return baseModelRunner;
	}

	private Model newModel() {
		try {
			return this.global.getClass().getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| SecurityException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
			return NGramModel.standard();
		}
	}
	
	private ModelRunner newModelRunner(Model model) {
		return new ModelRunner(model, this.lexerRunner, this.vocabulary);
	}

	/**
	 * When notified of a new file, update the nesting accordingly
	 */
	@Override
	public void notify(File next) {
		this.updateNesting(next);
	}

	// Defer all learning/forgetting to the global model; the nested part is update dynamically
	@Override
	public void learn(List<Integer> input) {
		this.global.learn(input);
	}
	@Override
	public void learnToken(List<Integer> input, int index) {
		this.global.learnToken(input, index);
	}
	@Override
	public void forget(List<Integer> input) {
		this.global.forget(input);
	}
	@Override
	public void forgetToken(List<Integer> input, int index) {
		this.global.forgetToken(input, index);
	}

	// Answer all modeling calls with the mixed model
	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		return this.mixed.modelToken(input, index);
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		return this.mixed.predictToken(input, index);
	}

	public Model getMix() {
		return this.mixed;
	}
	
	public File getTestRoot() {
		return this.files.get(0);
	}

	private void updateNesting(File next) {
		List<File> lineage = getLineage(next);
		// If lineage is empty, the current model is the (first meaningful) parent of next and is appropriate
		if (lineage == null || lineage.isEmpty()) return;
		int pos = 1;
		for (; pos < this.files.size(); pos++) {
			if (pos >= lineage.size() || !this.files.get(pos).equals(lineage.get(pos))) {
				this.modelRunners.get(pos - 1).learnDirectory(this.files.get(pos));
				this.files.subList(pos, this.files.size()).clear();
				this.modelRunners.subList(pos, this.modelRunners.size()).clear();
				break;
			}
		}
		for (int i = pos; i < lineage.size(); i++) {
			File file = lineage.get(i);
			Model model = newModel();
			this.files.add(file);
			this.modelRunners.add(newModelRunner(model));
			this.modelRunners.get(this.modelRunners.size() - 1).learnDirectory(file);
			this.modelRunners.get(this.modelRunners.size() - 2).forgetDirectory(file);
		}
		this.files.add(next);
		this.modelRunners.get(this.modelRunners.size() - 1).forgetDirectory(next);
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
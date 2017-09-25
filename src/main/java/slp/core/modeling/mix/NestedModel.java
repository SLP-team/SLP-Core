package slp.core.modeling.mix;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import slp.core.counting.Counter;
import slp.core.modeling.AbstractModel;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.ngram.NGramModel;
import slp.core.util.Pair;

public class NestedModel extends AbstractModel {

	private Model global;
	private List<Model> models;
	private List<File> files;
	
	private Model mixed;

	public NestedModel(File testRoot) {
		this(testRoot, NGramModel.standard());
	}
	
	public NestedModel(File testRoot, Model global) {
		this.global = global;
		Model local = fromGlobal(true);
		ModelRunner.learn(local, testRoot);

		this.files = new ArrayList<>();
		this.models = new ArrayList<>();
		this.files.add(testRoot);
		this.models.add(local);
		this.mixed = makeMix();
	}

	public NestedModel(File testRoot, Model global, Model local) {
		this.global = global;

		this.files = new ArrayList<>();
		this.models = new ArrayList<>();
		this.files.add(testRoot);
		this.models.add(local);
		this.mixed = makeMix();
	}

	private Model fromGlobal() {
		return fromGlobal(true);
	}

	private Model fromGlobal(boolean copyCounter) {
		try {
			if (copyCounter && this.global instanceof NGramModel) {
				Class<? extends Counter> counter = ((NGramModel) this.global).getCounter().getClass();
				return this.global.getClass().getConstructor(Counter.class).newInstance(counter.newInstance());
			}
			else {
				return this.global.getClass().newInstance();
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return NGramModel.standard();
		}
	}

	@Override
	public void notify(File next) {
		List<File> lineage = getLineage(next);
		// If lineage is empty, the current model is the (first meaningful) parent of next and is appropriate
		if (lineage == null || lineage.isEmpty()) return;
		int pos = 1;
		for (; pos < this.files.size(); pos++) {
			if (pos >= lineage.size() || !this.files.get(pos).equals(lineage.get(pos))) {
				ModelRunner.learn(this.models.get(pos - 1), this.files.get(pos));
				this.files.subList(pos, this.files.size()).clear();
				this.models.subList(pos, this.models.size()).clear();
				break;
			}
		}
		for (int i = pos; i < lineage.size(); i++) {
			File file = lineage.get(i);
			Model model = fromGlobal();
			this.files.add(file);
			this.models.add(model);
			ModelRunner.learn(model, file);
			ModelRunner.forget(this.models.get(this.models.size() - 2), file);
		}
		this.files.add(next);
		ModelRunner.forget(this.models.get(this.models.size() - 1), next);
		this.mixed = makeMix();
		this.mixed.notify(next);
	}

	/**
	 * Returns all non-trivial directories starting from the root file to the new file.
	 * Non-trivial meaning a directory containing more than one file/dir itself; 
	 * building a separate nested model for such directories is pointless.
	 * 
	 * @param file The next file to be modeled
	 * @return Path containing all relevant directories from root file inclusive to {@code file} exclusive
	 */
	private List<File> getLineage(File file) {
		List<File> lineage = new ArrayList<>();
		while (!file.getParentFile().equals(this.files.get(0))) {
			if (file.getParentFile().list().length > 1) {
				lineage.add(file.getParentFile());
			}
			file = file.getParentFile();
			if (file.getParentFile() == null) return null;
		}
		lineage.add(this.files.get(0));
		Collections.reverse(lineage);
		return lineage;
	}

	@Override
	public void learn(List<Integer> input) {
		// Tentatively, only the global model is updated dynamically
		this.global.learn(input);
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		// Tentatively, only the global model is updated dynamically
		this.global.learnToken(input, index);
	}

	@Override
	public void forget(List<Integer> input) {
		// Tentatively, only the global model is updated dynamically
		this.global.forget(input);
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		// Tentatively, only the global model is updated dynamically
		this.global.forgetToken(input, index);
	}

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		return this.mixed.modelToken(input, index);
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		return this.mixed.predictToken(input, index);
	}

	/**
	 * Make mix of {@code this.models} and {@code this.global}. Effective left-folds the list into a tree.
	 * @return a MixModel including all models currently alive.
	 */
	private Model makeMix() {
		Model mix = new InverseMixModel(this.global, this.models.get(0));
		for (int i = 1; i < this.models.size(); i++) {
			mix = new InverseMixModel(mix, this.models.get(i));
		}
		return mix;
	}
}

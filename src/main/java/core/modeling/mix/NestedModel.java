package core.modeling.mix;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import core.modeling.AbstractModel;
import core.modeling.Model;
import core.modeling.ModelRunner;
import core.modeling.ngram.NGramModel;
import core.util.Pair;

public class NestedModel extends AbstractModel {

	private Model global;
	private List<NGramModel> models;
	private List<File> files;
	
	private Model mixed;
	
	public NestedModel(File testRoot, Model global) {
		this.global = global;

		this.files = new ArrayList<>();
		this.models = new ArrayList<>();
		
		NGramModel local = NGramModel.standard();
		ModelRunner.learn(local, testRoot);
		this.files.add(testRoot);
		this.models.add(local);
		this.mixed = makeMix();
	}
	
	@Override
	public void notify(File next) {
		List<File> lineage = getLineage(next);
		// If lineage is empty, the current model is the (first meaningful) parent of next and is appropriate
		if (lineage.isEmpty()) return;
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
			NGramModel model = NGramModel.standard();
			this.files.add(file);
			this.models.add(model);
			ModelRunner.learn(model, file);
			ModelRunner.forget(this.models.get(this.models.size() - 2), file);
		}
		this.files.add(next);
		ModelRunner.forget(this.models.get(this.models.size() - 1), next);
		this.mixed = makeMix();
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
		}
		lineage.add(this.files.get(0));
		Collections.reverse(lineage);
		return lineage;
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		// Tentatively, only the global model is updated dynamically
		this.global.learnToken(input, index);
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		// Tentatively, only the global model is updated dynamically
		this.global.forgetToken(input, index);
	}

	@Override
	public Pair<Double, Double> modelToken(List<Integer> input, int index) {
		return this.mixed.modelToken(input, index);
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictToken(List<Integer> input, int index) {
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

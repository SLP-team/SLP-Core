package slp.core.modeling.misc;

import java.io.File;
import java.util.List;
import java.util.Map;

import slp.core.modeling.AbstractModel;
import slp.core.modeling.Model;
import slp.core.util.Pair;

public class CharacterModel extends AbstractModel {
	
	private final Model model;
	
	public CharacterModel(Model model) {
		this.model = model;
	}

	@Override
	public void notify(File next) {
		this.model.notify(next);
	}

	@Override
	public void learnToken(List<Integer> input, int index) {
		
	}

	@Override
	public void forgetToken(List<Integer> input, int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Pair<Double, Double> modelAtIndex(List<Integer> input, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, Pair<Double, Double>> predictAtIndex(List<Integer> input, int index) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}

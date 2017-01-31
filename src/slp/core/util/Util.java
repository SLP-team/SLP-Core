package slp.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Util {

	public static File getVocabularyFile(File countsRoot) {
		return new File(countsRoot, "vocab.out");
	}

	public static File getCounterFile(File countsRoot) {
		return new File(countsRoot, "counter.out");
	}
	
	public static List<File> getFiles(File project) {
		if (project.isFile()) return Collections.singletonList(project);
		List<File> files = new ArrayList<>();
		for (String child : project.list()) {
			File file = new File(project, child);
			if (file.isFile()) {
				files.add(file);
			}
			else if (file.isDirectory()) files.addAll(getFiles(file));
		}
		return files;
	}
}

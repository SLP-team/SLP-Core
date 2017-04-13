package slp.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Util {
	public static List<File> getFiles(File root) {
		if (root.isFile()) return Collections.singletonList(root);
		List<File> files = new ArrayList<>();
		for (String child : root.list()) {
			File file = new File(root, child);
			if (file.isFile()) {
				files.add(file);
			}
			else if (file.isDirectory()) files.addAll(getFiles(file));
		}
		return files;
	}
}

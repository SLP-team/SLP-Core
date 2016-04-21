package slp.core.tokenizing;

import java.io.Serializable;

public class Token implements Serializable {
	
	private static final long serialVersionUID = 2836909126359379086L;

	private final String text;
	
	public Token(String text) {
		this.text = text;
	}
	
	public String text() {
		return this.text;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		else if (other == null) return false;
		else if (!(other instanceof Token)) return false;
		else return this.text.equals(((Token) other).text());
	}
	
	@Override
	public int hashCode() {
		return this.text.hashCode();
	}
	
	@Override
	public String toString() {
		return this.text;
	}
}

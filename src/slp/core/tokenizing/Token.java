package slp.core.tokenizing;

public class Token {
	
	private final String text;
	
	public Token(String text) {
		this.text = text;
	}
	
	public String text() {
		return this.text;
	}
}

//package example.fuzzer;

import com.gargoylesoftware.htmlunit.html.*;
import java.net.URL;
import java.util.HashMap;
import java.util.ArrayList;

public class FileNode {

	private HtmlPage page;
	private URL url;
	private HashMap<String, ArrayList<String>> queries;
	
	public FileNode( HtmlPage page ) {
		this.page = page;
		url = page.getUrl();
		queries = new HashMap<String, ArrayList<String>>();
	}
	
	public void addQuery( String query ) {
		//TODO
	}
}
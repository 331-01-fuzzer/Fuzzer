package example.fuzzer;

import com.gargoylesoftware.htmlunit.html.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	public ArrayList<URL> getLinks() {
		ArrayList<URL> links = new ArrayList<URL>();
		for( HtmlAnchor link : page.getAnchors() ) {
			System.out.println( "FileNode found URL: " + link.getHrefAttribute() );
			try {
				links.add( new URL( link.getHrefAttribute() ) );
			} catch( MalformedURLException e ) {
				// just don't add it
			}
		}
		return links;
	}
	
	public void printResults() {
		System.out.println( url );
		//TODO print queries
		//TODO print form inputs
	}
}
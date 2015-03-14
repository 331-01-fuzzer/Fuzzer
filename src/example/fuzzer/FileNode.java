package example.fuzzer;

import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class FileNode {

	private HtmlPage page;
	private URL url;
	private HashMap<String, ArrayList<String>> queries;
	
	public FileNode( HtmlPage page ) {
		this.page = page;
		try {
			url = new URL( page.getUrl().toString().split( "\\?" )[0] );
		} catch( MalformedURLException e ) {
			url = page.getUrl(); // just do the best we can
		}
		queries = new HashMap<String, ArrayList<String>>();
		addQuery( page.getUrl().getQuery() );
	}
	
	public void addQuery( String query ) {
		if( query != null && !"".equals( query ) ) {
			System.out.println( query );
			String[] params = query.split( "&" );
			for( int i = 0; i < params.length; ++i ) {
				String[] param = params[i].split( "=" );
				if( !queries.containsKey( param[0] ) )
					queries.put( param[0], new ArrayList<String>() );
				if( !queries.get( param[0] ).contains( param[1] ) )
					queries.get( param[0] ).add( param[1] );
			}
		}
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
	
	@SuppressWarnings( "unchecked" )
	public void tryForms( List<String> vectors ) {
		List<HtmlForm> forms = page.getForms();
		for( HtmlForm form : forms ) {
			List<HtmlInput> inputs = (List<HtmlInput>) form.getByXPath( "//textarea" );
			inputs.addAll( (List<HtmlInput>) form.getByXPath( "//input" ) );
			HtmlElement submit = form.getFirstByXPath( "//input[@type='submit']" );
			if( submit == null ) submit = form.getFirstByXPath( "//button" );
			for( ListIterator<String> it = vectors.listIterator(); it.hasNext(); ) {
				for( HtmlInput input : inputs ) {
					if( it.hasNext() ) {
						input.setValueAttribute( it.next() );
					} else { // reached the end of vectors,
						// but still have inputs to fill in
						input.setValueAttribute( it.previous() );
					}
				}
				try {
					WebResponse response = submit.<HtmlPage> click().getWebResponse();
					//TODO check response
				} catch( IOException e ) {
					//TODO does that mean we got a bad response?
				}
				//TODO gather results and do something with them (return/print/file)
			}
		}
	}
	
	public void tryUrlParams( List<String> vectors ) {
		//TODO
	}
	
	public void checkSensitiveInfo( List<String> keywords ) {
		//TODO
	}
	
	public void printResults() {
		System.out.println( url );
		for( String param : queries.keySet() )
			System.out.println( "\t" + param );
		//TODO print form inputs
	}
}
package example.fuzzer;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.*;
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
	private List<HtmlForm> forms;
	
	public FileNode( HtmlPage page ) {
		this.page = page;
		try {
			url = new URL( page.getUrl().toString().split( "\\?" )[0] );
		} catch( MalformedURLException e ) {
			url = page.getUrl(); // just do the best we can
		}
		queries = new HashMap<String, ArrayList<String>>();
		addQuery( page.getUrl().getQuery() );
		forms = page.getForms();
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
	
	public void tryUrlParams( WebClient client, List<String> vectors, List<String> keywords ) {
		if( !queries.isEmpty() ) {
			for( String vector : vectors ) {
				String query = "?";
				for( String param : queries.keySet() ) {
					if( query.length() > 1 ) query += "&";
					query += param + "=" + vector;
				}
				try {
					HtmlPage page = client.getPage( url + query );
					checkSensitiveInfo( page.asXml(), vector, keywords );
				} catch( FailingHttpStatusCodeException e ) {
					System.out.println( "\tBad response (" + e.getStatusCode() + ") for vector " + vector );
				} catch( IOException e ) {
					//FIXME what would cause this that's not a FailingHttpStatusCodeException?
				} catch( ScriptException e ) {
					// don't care
				}
			}
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public void tryForms( List<String> vectors, List<String> keywords ) {
		for( HtmlForm form : forms ) {
			List<HtmlInput> inputs = (List<HtmlInput>) form.getByXPath( "//textarea" );
			inputs.addAll( (List<HtmlInput>) form.getByXPath( "//input" ) );
			HtmlElement submit = form.getFirstByXPath( "//input[@type='submit']" );
			if( submit == null ) submit = form.getFirstByXPath( "//button" );
			if( submit == null ) {
				// can't really do anything if we can't figure out how to submit
			} else {
				for( String vector : vectors ) {
					for( HtmlInput input : inputs )
						input.setValueAttribute( vector );
					try {
						String response = submit.<HtmlPage> click().getWebResponse().getContentAsString();
						checkSensitiveInfo( response, vector, keywords );
					} catch( IOException e ) {
						System.out.println( "\tBad response for vector " + vector );
						//TODO get status code from the IOException?
						//TODO get the html result to scan for keywords?
					}
				}
			}
		}
	}
	
	// helper for tryForms and tryUrlParams
	private void checkSensitiveInfo( String response, String vector, List<String> keywords ) {
		// check for unsanitized input
		if( response.indexOf( vector ) != -1 )
			System.out.println( "\tUnsanitized input: " + vector );
			
		// check for information leak
		for( String s : keywords )
			if( response.indexOf( s ) != -1 )
				System.out.println( "\tLeaked information: " + s );
	}
	
	@SuppressWarnings( "unchecked" )
	public void printResults() {
		// print url
		System.out.println( url );
		
		// print url query parameters
		if( !queries.isEmpty() ) {
			System.out.println( "\tURL Query Parameters:" );
			for( String param : queries.keySet() )
				System.out.println( "\t--" + param );
		}
		
		// print form inputs
		if( !forms.isEmpty() ) {
			System.out.println( "\tForm Inputs:" );
			for( HtmlForm form : forms ) {
				for( HtmlTextArea ta : (List<HtmlTextArea>) form.getByXPath( "//textarea" ) )
					System.out.println( "".equals( ta.getNameAttribute() ) ? "[no name]" : ta.getNameAttribute() );
				for( HtmlInput in : (List<HtmlInput>) form.getByXPath( "//input" ) )
					System.out.println( "".equals( in.getNameAttribute() ) ? "[no name]" : in.getNameAttribute() );
			}
		}
	}
}
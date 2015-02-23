//package example.fuzzer;

import com.gargoylesoftware.htmlunit.html.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

public class PathNode {
	
	private URL url;
	private URI uri;
	private HashMap<String, PathNode> subpaths;
	private HashMap<String, FileNode> files;
	
	public PathNode( URL url ) throws MalformedURLException {
		this.url = url;
		try {
			uri = url.toURI();
		} catch( URISyntaxException e ) {
			throw new MalformedURLException( e.getMessage() );
		}
		subpaths = new HashMap<String, PathNode>();
		files = new HashMap<String, FileNode>();
	}
	
	/**
	 * @return true iff this is a new page
	 */
	public boolean addPage( HtmlPage page ) throws MalformedURLException {
		try {
			URI rel = page.getUrl().toURI().relativize( uri );
			System.out.println( "rel: " + rel );
			String[] parts = rel.getPath().split( "/" );
			if( parts.length > 1 ) {
				if( !subpaths.containsKey( parts[0] ) ) {
					subpaths.put( parts[0], new PathNode( uri.resolve( parts[0] ).toURL() ) );
				}
				return subpaths.get( parts[0] ).addPage( page );
			} else {
				boolean added = false;
				if( !files.containsKey( parts[0] ) ) {
					files.put( parts[0], new FileNode( page ) );
					added = true;
				}
				files.get( parts[0] ).addQuery( rel.getQuery() );
				return added;
			}
		} catch( URISyntaxException e ) {
			throw new MalformedURLException( e.getMessage() );
		}
	}
}
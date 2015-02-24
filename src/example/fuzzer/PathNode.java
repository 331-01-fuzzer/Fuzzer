package example.fuzzer;

import com.gargoylesoftware.htmlunit.html.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
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
	 * @return list of newly discovered (or guessed) URLs
	 */
	public ArrayList<URL> addPage( HtmlPage page ) throws MalformedURLException {
		try {
			URI rel = page.getUrl().toURI().relativize( uri );
			System.out.println( "rel: " + rel );
			String[] parts = rel.getPath().split( "/" );
			if( parts.length > 1 ) {
				if( !subpaths.containsKey( parts[0] ) ) {
					subpaths.put( parts[0], new PathNode( uri.resolve( parts[0] ).toURL() ) );
					//TODO page guessing (append filenames/extensions to current path), and include results in return list
				}
				return subpaths.get( parts[0] ).addPage( page );
			} else {
				if( !files.containsKey( parts[0] ) ) {
					FileNode node = new FileNode( page );
					files.put( parts[0], node );
					return node.getLinks();
					//TODO do something with form inputs
				}
				files.get( parts[0] ).addQuery( rel.getQuery() );
				return null;
				//TODO do something with query parameters (or do that later, since we don't necessarily know all of them yet)
			}
		} catch( URISyntaxException e ) {
			throw new MalformedURLException( e.getMessage() );
		}
	}
}
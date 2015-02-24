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
			// get relative uri
			URI rel = uri.relativize( page.getUrl().toURI() );
			if( rel.equals( page.getUrl().toURI() ) ) return new ArrayList<URL>(); // not really a subpath or file on this part of the tree.
			// get first part
			String[] parts = rel.getPath().split( "/" );
			/*
			"" = [""] (1) idx=1
			"file" = ["file"] (1) idx=0
			"/file" = ["","file"] (2) idx=1
			"path/etc" ["path","etc"] (2) idx=0
			"/path/etc" ["","path","etc"] (3) idx=1
			*/
			int idx = "".equals( parts[0] ) ? 1 : 0;
			if( parts.length > idx + 1 ) { // this is a path (i.e. [/]something/somethingelse)
				ArrayList<URL> urls = new ArrayList<URL>();
				if( !subpaths.containsKey( parts[idx] ) ) {
					// haven't seen this path yet. add it to the tree and perform guessing
					PathNode node = new PathNode( new URL( uri + (uri.toString().endsWith( "/" ) ? "" : "/") + parts[idx] ) );
					subpaths.put( parts[idx], node );
					urls = node.getGuesses();
				}
				// add the subpath and return the result
				urls.addAll( subpaths.get( parts[idx] ).addPage( page ) );
				return urls;
			} else { // this is a file (i.e. [/]something[.someext][?somequery])
				idx = parts.length - 1; // fix case ""
				if( !files.containsKey( parts[idx] ) ) {
					// haven't seen this file yet. add it to the tree and scrape its links
					FileNode node = new FileNode( page );
					files.put( parts[idx], node );
					return node.getLinks();
					//TODO do something with form inputs
				}
				// already seen this file. just add any new query parameters.
				files.get( parts[idx] ).addQuery( rel.getQuery() );
				return new ArrayList<URL>(); // empty arraylist so we don't have to worry about passing nulls around
				//TODO do something with query parameters (or do that later, since we don't necessarily know all of them yet)
			}
		} catch( URISyntaxException e ) {
			// if there's a problem with the URI, it's because there was a problem with the URL.
			throw new MalformedURLException( e.getMessage() );
		}
	}
	
	/**
	 * @return list of URLs to guess
	 */
	public ArrayList<URL> getGuesses() {
		ArrayList<URL> guesses = new ArrayList<URL>();
		if( BasicFuzzer.commonWords != null ) {
			for( String word : BasicFuzzer.commonWords ) {
				if( word.startsWith( "/" ) ) word = word.substring(1);
				try {
					System.out.println( "Guessing URL: " + uri.resolve( word ).toURL() );
					guesses.add( uri.resolve( word ).toURL() );
				} catch( MalformedURLException e ) {
					// just don't guess it
				}
			}
		}
		return guesses;
	}
	
	/**
	 * Prints all urls, including children
	 */
	public void printResults() {
		for( FileNode file : files.values() ) file.printResults();
		for( PathNode path : subpaths.values() ) path.printResults();
	}
}
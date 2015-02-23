public class SiteNode {
	
	private URL url;
	private URI uri;
	private HashMap<String, PathNode> subpaths;
	private HashMap<String, FileNode> files;
	
	public PathNode( URL url ) {
		this.url = url;
		uri = url.toURI();
		subpaths = new HashMap<String, PathNode>();
		files = new HashMap<String, FileNode>();
	}
	
	/**
	 * @return true iff this is a new page
	 */
	public boolean addPage( HtmlPage page ) {
		URL rel = page.getUrl().toURI().relativize( uri ).toURL();
		String[] parts = String.split( rel.getPath(), '/' );
		if( parts.length > 1 ) {
			if( !subpaths.containsKey( parts[0] ) ) {
				subpaths.put( parts[0], new PathNode( uri.resolve( parts[0] ) ) );
			}
			return subpaths.get( parts[0] ).addPage( page );
		} else {
			boolean added = false;
			if( !files.containsKey( parts[0] ) ) {
				files.put( parts[0], new FileNode( page ) );
				added = true;
			}
			files.get( parts[0] ).addQueries( rel.getQuery() );
			return added;
		}
	}
}
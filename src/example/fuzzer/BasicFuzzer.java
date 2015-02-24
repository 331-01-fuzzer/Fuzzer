package example.fuzzer;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

public class BasicFuzzer {

  /*
  Possible flags:
    --custom-auth=string     Signal that the fuzzer should use hard-coded authentication for a specific application (e.g. dvwa). Optional.

  Discover options:
    --common-words=file    Newline-delimited file of common words to be used in page guessing and input guessing. Required.

  Test options:
    --vectors=file         Newline-delimited file of common exploits to vulnerabilities. Required.
    --sensitive=file       Newline-delimited file data that should never be leaked. It's assumed that this data is in the application's database (e.g. test data), but is not reported in any response. Required.
    --random=[true|false]  When off, try each input to each page systematically.  When on, choose a random page, then a random input field and test all vectors. Default: false.
    --slow=500             Number of milliseconds considered when a response is considered "slow". Default is 500 milliseconds
   */
  static Map<String,String> flags = new HashMap<String,String>();
  
  /**
   *
   * @param args
   * @throws FailingHttpStatusCodeException
   * @throws MalformedURLException
   * @throws IOException
   */

  //   fuzz test http://localhost:8080 --custom-auth=dvwa --common-words=words.txt --vectors=vectors.txt --sensitive=creditcards.txt --random=false --slow=500
  public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    if(args.length<2) {
      System.err.println("Too few args.");
      System.exit(1);
    }

    String[] loginDetails = new String[2];

    String runType = args[0];
    String url = args[1];

    System.out.println("Running fuzzer type \"" + runType + "\" on url " + url);

    if(args.length > 2) readFlags(Arrays.copyOfRange(args, 2, args.length));

    if(flags.containsKey("--custom-auth")) loginDetails = getAuth(flags.get("--custom-auth"));
    else {
      loginDetails[0] = "";
      loginDetails[1] = "";
    }

    WebClient webClient = new WebClient();
    webClient.setJavaScriptEnabled(true);

    if("discover".equals(runType.toLowerCase())) {
      runDiscover(webClient, url);
    } else if("test".equals(runType.toLowerCase())) {
      runTest();
    }

//		doFormPost(webClient);
  }

  private static void readFlags(String[] args) {
    for(String s: args) {
      String[] kv = s.split("=");
      flags.put(kv[0],kv[1]);
    }
  }

  private static String[] getAuth(String customAuth) {
    String[] loginDetails = new String[2];

    if("dvwa".equals(customAuth.toLowerCase())) {
      loginDetails[0]="admin";
      loginDetails[1]="password";
    } else if("bodgeit".equals(customAuth.toLowerCase())) {
      loginDetails[0]="admin";
      loginDetails[1]="1' OR '1'='1";
    } else {
      loginDetails[0]="";
      loginDetails[1]="";
    }

    return loginDetails;
  }

  private static List<String> readFile(String fileName) throws IOException {
    List<String> lines = new ArrayList<String>();

    FileReader in = new FileReader(fileName);
    BufferedReader br = new BufferedReader(in);

    String line="";
    while((line = br.readLine()) != null) {
      lines.add(line);
    }

    return lines;
  }

  private static void runDiscover(WebClient client, String url) throws IOException, MalformedURLException {
    discoverLinks(client, url);
    client.closeAllWindows();
  }

  private static void runTest() {
    //TODO: release 2
  }
  
  /**
   * This code is for showing how you can get all the links on a given page, and visit a given URL
   * @param webClient
   * @throws IOException
   * @throws MalformedURLException
   */
  private static void discoverLinks(WebClient webClient, String url) throws IOException, MalformedURLException {
    HtmlPage page = webClient.getPage(url);
    PathNode root = new PathNode(page.getUrl());
	root.addPage(page);
    List<HtmlAnchor> links = page.getAnchors();
    for(HtmlAnchor link : links) {
	  //TODO add them, and iterate
      if (page.getUrl().equals(link.getHrefAttribute())){
    	  System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
      }
    }
  }

  /**
   * This code is for demonstrating techniques for submitting an HTML form. Fuzzer code would need to be
   * more generalized
   * @param webClient
   * @throws FailingHttpStatusCodeException
   * @throws MalformedURLException
   * @throws IOException
   */
  private static void doFormPost(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
    List<HtmlForm> forms = page.getForms();
    for (HtmlForm form : forms) {
      HtmlInput input = form.getInputByName("quantity");
      input.setValueAttribute("2");
      HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath("//input[@id='submit']");
      System.out.println(submit.<HtmlPage> click().getWebResponse().getContentAsString());
    }
  }
}

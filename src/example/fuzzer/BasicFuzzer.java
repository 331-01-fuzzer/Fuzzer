package example.fuzzer;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
  static WebClient webClient;
  
  static List<String> commonWords;
  
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

    java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF); 
    webClient = new WebClient();
    webClient.setJavaScriptEnabled(true);
    webClient.setThrowExceptionOnFailingStatusCode(false);

    if(flags.containsKey("--custom-auth")) getAuth(flags.get("--custom-auth"));
	if(flags.containsKey("--common-words")) commonWords = readFile(flags.get("--common-words"));

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

  private static void getAuth(String customAuth) {
    HtmlPage logIn = null;
    HtmlForm logInForm = null;

    if ("dvwa".equals(customAuth.toLowerCase())) {
      try {
        logIn = webClient.getPage("http://127.0.0.1/dvwa/login.php");
      } catch (Exception e) {
        e.printStackTrace();
      }

      logInForm = logIn.getFirstByXPath("/html/body/div/form");
      logInForm.getInputByName("username").setValueAttribute("admin");
      logInForm.getInputByName("password").setValueAttribute("password");

      try {
        logInForm.getInputByName("Login").click();
      } catch (Exception e) {
        e.printStackTrace();
      }

    } else if ("bodgeit".equals(customAuth.toLowerCase())) {
      try {
        logIn = webClient.getPage("http://127.0.0.1:8080/bodgeit/register.jsp");
      }catch (Exception e) {
        e.printStackTrace();
      }

      logInForm = logIn.getFirstByXPath(".//form[@method='POST']");
      logInForm.getInputByName("username").setValueAttribute("fakeuser@fakemail.com");
      logInForm.getInputByName("password1").setValueAttribute("pass");
      logInForm.getInputByName("password2").setValueAttribute("pass");


      try {
        logInForm.getInputByValue("Register").click();
      }catch(IOException e) {
        e.printStackTrace();
      }

    } else {
      System.out.println("No credentials available for " +customAuth+ ". Continuing the fuzz without login");
    }
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
	discoverInputs(client, url);
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
  private static void discoverLinks(WebClient webClient, String homeurl) throws IOException, MalformedURLException {
    HtmlPage homepage = webClient.getPage(homeurl);
    PathNode root = new PathNode(homepage.getUrl());
	ArrayDeque<URL> queue = new ArrayDeque<URL>(root.getGuesses());
	queue.addAll(root.addPage(homepage));
	while(!queue.isEmpty()) {
	  URL url = queue.remove();
	  if(homepage.getUrl().getHost().equals(url.getHost())) {
        try {
	      System.out.println( "trying url: " + url );
          HtmlPage page = webClient.getPage(url);
          queue.addAll(root.addPage(page));
        } catch( FailingHttpStatusCodeException e ) {
	      // error getting page (most likely it doesn't exist) - don't add it.
	    } catch( ScriptException e ) {
		  //FIXME should probably still add it? or would it not be a valid HtmlPage?
		}
      }
	}
	
	System.out.println( "\nDiscovery Results:\n" );
	root.printResults();
  }
  
  /**
   * This code is for showing how you can get all the links on a given page, and visit a given URL
   * @param webClient
   * @throws IOException
   * @throws MalformedURLException
   */
  private static void discoverInputs(WebClient webClient, String url) throws IOException, MalformedURLException {
    HtmlPage page = webClient.getPage(url);
    //PathNode root = new PathNode(page.getUrl());
	//root.addPage(page);
    List<HtmlForm> forms = page.getForms();
    Set<Cookie> cookies = webClient.getCookieManager().getCookies(page.getUrl());
    for(HtmlForm form : forms) {
    	System.out.println("Input discovered: " + form.asText());
    }
    for(Cookie cookie : cookies){
    	System.out.println("Cookie discovered: " + cookie.toString());
    }
  }

  /**
   *
   */
  private static void getResponseCode(String url) throws IOException, MalformedURLException {
    int statusCode = webClient.getPage(url).getWebResponse().getStatusCode();
    if (statusCode!=200) {
      System.out.println("Status code for "+url+" not 200 OK and is instead " +statusCode);
    }
  }

  /**
   *
   */
  private static void checkResponseTime(String url) throws IOException, MalformedURLException {
    long responseTime = webClient.getPage(url).getWebResponse().getLoadTime();
    if(Integer.parseInt(flags.get("--slow")) < responseTime) {
      System.out.println("Response time of "+responseTime+"ms for website " + url + " slower than expected response time " +Integer.parseInt(flags.get("--slow"))+ "ms");
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

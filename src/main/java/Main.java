import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.w3c.dom.Element;

import ch.qos.logback.classic.Logger;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnPopupWindowPlugin;
import com.crawljax.core.state.StateVertex;
import com.crawljax.plugins.crawloverview.CrawlOverview;


public class Main {
	
	private static final long WAIT_TIME_AFTER_EVENT = 200;
	private static final long WAIT_TIME_AFTER_RELOAD = 20;
	
	private static final String[] signupKeywords = 
		{"sign up", "signup", "sign in", "signin", "log in", "login", "connect", "register", "account", "continue with", 
		"google", "facebook", "twitter"};

	public static void main(String[] args) throws FileNotFoundException {
		
		Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
		//root.setLevel(ch.qos.logback.classic.Level.ERROR);
		
		System.setProperty("phantomjs.binary.path", 
				"/Users/user/Documents/ws_commercenet/PhantomCrawler/phantomjs-1.9.7-macosx/bin/phantomjs");
		final PrintWriter out = new PrintWriter("out.txt");
		
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor("http://doubledowncasino.com");
		
		builder.setOutputDirectory(new File("res_before_login2"));
		
		
		// browser
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.PHANTOMJS, 4));
		// depth
		builder.setMaximumDepth(3);
		// rules
		builder.crawlRules().clickElementsInRandomOrder(true);
		builder.crawlRules().followExternalLinks(false);
		// elements
		builder.crawlRules().clickDefaultElements();
		builder.crawlRules().click("span");
		// timeouts
		builder.crawlRules().waitAfterReloadUrl(WAIT_TIME_AFTER_RELOAD, TimeUnit.MILLISECONDS);
		builder.crawlRules().waitAfterEvent(WAIT_TIME_AFTER_EVENT, TimeUnit.MILLISECONDS);
		
		/*
		// filtering candidates
		builder.addPlugin(new PreStateCrawlingPlugin() {

			public void preStateCrawling(CrawlerContext context, 
					ImmutableList<CandidateElement> candidateElements, StateVertex state) {
								
				LinkedList<CandidateElement> newCandidates = new LinkedList<CandidateElement>();
				for (CandidateElement e : candidateElements) {
					
					out.println(e.getElement().getTagName());
					out.flush();
					
					if (!"SPAN".equals(e.getElement().getTagName())) {
						newCandidates.add(e);
					}
					else if (isSignupRelated(e.getElement())) {
						
						//out.println(e.getElement().getTagName());
						//out.flush();
						
						newCandidates.add(e);
					}
				}
				
				state.setElementsFound(newCandidates);
			}
		});
		*/
		
		builder.addPlugin(new CrawlOverview());
		
		// Printing states
		builder.addPlugin(new OnNewStatePlugin() {

			public void onNewState(CrawlerContext context, StateVertex newState) {
				
				out.println(newState.getUrl());
				out.flush();
			}
		});
		
		// Detecting Fb login
		builder.addPlugin(new OnPopupWindowPlugin() {

			public void onPopupWindow(CrawlerContext context) {
				
				WebDriver browser = context.getBrowser().getBrowser();
				
				if (browser.getWindowHandles().size() > 1) {
				
					String current = browser.getWindowHandle();
					
					for (String s : browser.getWindowHandles()) {
						if (s.equals(current)) continue;
						
						browser.switchTo().window(s);
						
						if (browser.getCurrentUrl().startsWith("https://www.facebook.com/login.php")) {
								
							out.println(" >>>>> " + browser.getCurrentUrl());
							out.flush();
							// todo
							
						}
							
						browser.switchTo().window(current);
					}
				}
			}
		});
		
		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();

		
		out.close();
	}
	
	private static boolean isSignupRelated(Element el) {
		
		String text = el.getTextContent();
		if (text != null) {
			text = text.toLowerCase().replaceAll("[^A-Za-z]", " ");
			for (String k : signupKeywords) {
				if (text.contains(k)) return true;
			}
		}
		
		return false;
	}

}

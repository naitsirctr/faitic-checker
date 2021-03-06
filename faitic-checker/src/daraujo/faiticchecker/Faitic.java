/*
 * 	Faicheck - A NON OFFICIAL application to manage the Faitic Platform
 * 	Copyright (C) 2016, 2017 David Ricardo Araújo Piñeiro
 * 	
 * 	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 * 	
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 * 	
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package daraujo.faiticchecker;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class Faitic {


	private static final String urlMain="https://faitic.uvigo.es/index.php/es/";
	private static final String urlSubjects="https://faitic.uvigo.es/index.php/es/materias";
	private static CookieManager cookieManager;
	public static Logger logger;
	
	public Faitic(boolean verbose){
		toDoAtStartup(verbose);
	}
	
	private static void toDoAtStartup(boolean verbose){
		
		startCookieSession();
		logger=new Logger(verbose);
		
	}
	
	private static void startCookieSession(){

		cookieManager=new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
		cookieManager.getCookieStore().removeAll();
	
		//CookieHandler.setDefault(cookieManager);
		
	
	}
	
	public static String lastRequestedURL="";
	
	public static String requestDocument(String strurl, String post) throws Exception{

		lastRequestedURL=strurl;
		
		logger.log(Logger.INFO, "Requesting URL: " + strurl);
		logger.log(Logger.INFO, "Post data: " + post);

		logger.log(Logger.INFO, "--- Creating connection ---");
		
		URL url=new URL(strurl);

		List<HttpCookie> cookiesAssoc=cookieManager.getCookieStore().get(url.toURI());
		String cookiesAssocStr="";

		for(HttpCookie cookieAssoc : cookiesAssoc){

			cookiesAssocStr+=(cookiesAssocStr.length()>0 ? "; " : "") + cookieAssoc.getName() + "=" + cookieAssoc.getValue();

		}

		HttpURLConnection connection= (HttpURLConnection) url.openConnection();

		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setUseCaches(false);

		connection.setRequestProperty("Accept-Encoding", "gzip");
		
		if(post.length()>0){
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			//connection.setRequestProperty("charset", "utf-8");
			//connection.setRequestProperty("Content-Length", "" + post.length());
		}
		
		if(cookiesAssocStr.length()>0){
			connection.setRequestProperty("Cookie", cookiesAssocStr);
			logger.log(Logger.INFO, "Cookies: " + cookiesAssocStr);
		}
		
		DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(post.getBytes(StandardCharsets.UTF_8));
		
		logger.log(Logger.INFO, "--- Petition sent. Reading ---");
		
		StringBuffer output=new StringBuffer();
		InputStream reader;
		
		if(connection.getContentEncoding().equals("gzip")){
			
			reader=new GZIPInputStream(connection.getInputStream());
			logger.log(Logger.INFO, " + GZIP ENCODED");
			
		}
		else{
			
			reader = connection.getInputStream();
			
		}

		byte[] temp = new byte[1000];
		int read = reader.read(temp);

			int counter=0;

		while (read != -1) {
			output.append(new String(temp,0,read,StandardCharsets.UTF_8));
			read = reader.read(temp);
			counter+=read;

		}

		reader.close();
		
		int status=connection.getResponseCode();
		
		String headerName;
		
		for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
			
			if(headerName.toLowerCase().equals("set-cookie")){
				
				String cookiesToSet=connection.getHeaderField(i);
				
				for(String cookieToSet : cookiesToSet.split(";")){
					
					String[] cookieParameters=cookieToSet.split("=");
					
					cookieManager.getCookieStore().add(url.toURI(), new HttpCookie(cookieParameters[0].trim(), cookieParameters[1].trim()));
					
					logger.log(Logger.INFO, " + Adding cookie \"" + cookieToSet + "\" to uri \"" + url.toURI().toString() + "\".");
					
				}
				
				
			}
			
		}
		
		if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER){
				

				logger.log(Logger.INFO, "--- Redirected ---");
				
				return requestDocument(connection.getHeaderField("Location"),"");
				
				
		}
			
			else{
				
				logger.log(Logger.INFO, "--- Request finished ---\n");
				
				return output.toString();

			}
		
	}

	public static void downloadFile(String strurl, String post, String filename) throws Exception{

		lastRequestedURL=strurl;
		
		logger.log(Logger.INFO, "Requesting URL: " + strurl);
		logger.log(Logger.INFO, "Post data: " + post);

		logger.log(Logger.INFO, "--- Creating connection ---");
		
		URL url=new URL(strurl);

		List<HttpCookie> cookiesAssoc=cookieManager.getCookieStore().get(url.toURI());
		String cookiesAssocStr="";

		for(HttpCookie cookieAssoc : cookiesAssoc){

			cookiesAssocStr+=(cookiesAssocStr.length()>0 ? "; " : "") + cookieAssoc.getName() + "=" + cookieAssoc.getValue();

		}

		HttpURLConnection connection= (HttpURLConnection) url.openConnection();

		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setUseCaches(false);

		if(post.length()>0){
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			//connection.setRequestProperty("charset", "utf-8");
			//connection.setRequestProperty("Content-Length", "" + post.length());
		}
		
		if(cookiesAssocStr.length()>0){
			connection.setRequestProperty("Cookie", cookiesAssocStr);
			logger.log(Logger.INFO, "Cookies: " + cookiesAssocStr);
		}
		
		DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(post.getBytes(StandardCharsets.UTF_8));
		
		logger.log(Logger.INFO, "--- Petition sent. Reading ---");
		
		InputStream reader;	// Response document
		
		reader = connection.getInputStream();

		logger.log(Logger.INFO, " + Saving as: " + filename);
		
		FileOutputStream filewriter = new FileOutputStream(filename);

		byte[] temp = new byte[1000];
		int read = reader.read(temp);

		while (read != -1) {
			filewriter.write(temp, 0, read);
			
			read = reader.read(temp);
			
		}

		reader.close();
		filewriter.close();
		
		int status=connection.getResponseCode();
		
		String headerName;
		
		for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
			
			if(headerName.toLowerCase().equals("set-cookie")){
				
				String cookiesToSet=connection.getHeaderField(i);
				
				for(String cookieToSet : cookiesToSet.split(";")){
					
					String[] cookieParameters=cookieToSet.split("=");
					
					cookieManager.getCookieStore().add(url.toURI(), new HttpCookie(cookieParameters[0].trim(), cookieParameters[1].trim()));
					
					logger.log(Logger.INFO, " + Adding cookie \"" + cookieToSet + "\" to uri \"" + url.toURI().toString() + "\".");
					
				}
				
				
			}
			
		}
		
		if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER){
				

				logger.log(Logger.INFO, "--- Redirected ---");
				
				downloadFile(connection.getHeaderField("Location"),"", filename);
				
				return;
				
		}
			
			else{
				
				logger.log(Logger.INFO, "--- Request finished ---\n");
				
				
				return;

			}
		
	}

	public static String getRedirectedURL(String strurl, String post) throws Exception{

		lastRequestedURL=strurl;
		
		logger.log(Logger.INFO, "Requesting URL: " + strurl);
		logger.log(Logger.INFO, "Post data: " + post);

		logger.log(Logger.INFO, "--- Creating connection ---");
		
		URL url=new URL(strurl);

		List<HttpCookie> cookiesAssoc=cookieManager.getCookieStore().get(url.toURI());
		String cookiesAssocStr="";

		for(HttpCookie cookieAssoc : cookiesAssoc){

			cookiesAssocStr+=(cookiesAssocStr.length()>0 ? "; " : "") + cookieAssoc.getName() + "=" + cookieAssoc.getValue();

		}

		HttpURLConnection connection= (HttpURLConnection) url.openConnection();

		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setUseCaches(false);

		connection.setRequestProperty("Accept-Encoding", "gzip");
		
		if(post.length()>0){
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			//connection.setRequestProperty("charset", "utf-8");
			//connection.setRequestProperty("Content-Length", "" + post.length());
		}
		
		if(cookiesAssocStr.length()>0){
			connection.setRequestProperty("Cookie", cookiesAssocStr);
			logger.log(Logger.INFO, "Cookies: " + cookiesAssocStr);
		}
		
		DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
		writer.write(post.getBytes(StandardCharsets.UTF_8));
		
		logger.log(Logger.INFO, "--- Petition sent. Waiting for redirecting info ---");
		
		int status=connection.getResponseCode();
		
		String headerName;
		
		// Getting cookies
		
		for (int i=1; (headerName = connection.getHeaderFieldKey(i))!=null; i++) {
			
			if(headerName.toLowerCase().equals("set-cookie")){
				
				String cookiesToSet=connection.getHeaderField(i);
				
				for(String cookieToSet : cookiesToSet.split(";")){
					
					String[] cookieParameters=cookieToSet.split("=");
					
					cookieManager.getCookieStore().add(url.toURI(), new HttpCookie(cookieParameters[0].trim(), cookieParameters[1].trim()));
					
					logger.log(Logger.INFO, " + Adding cookie \"" + cookieToSet + "\" to uri \"" + url.toURI().toString() + "\".");
					
				}
				
				
			}
			
		}
		
		// Return status, there will be the redirection
		
		if (status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER){
				

				logger.log(Logger.INFO, "--- Redirected. ---");
				
				String redURL=connection.getHeaderField("Location");
				
				logger.log(Logger.INFO, "URL: " + redURL);
				return redURL;
				
				
		}
			
			else{
				
				logger.log(Logger.INFO, "--- Request finished. Not redirected ---\n");
				
				return null;

			}
		
	}

	public static String generatePostLogin(String username, String password) throws Exception{
		
		StringBuffer output=new StringBuffer();
		
		String documentMain=requestDocument(urlMain,"");
		
		int formStart=documentMain.toLowerCase().indexOf("<form action=\"/index.php/es/\" method=\"post\" id=\"login-form\"");
		
		int formEnd=documentMain.toLowerCase().indexOf("</form>", formStart);
		
		// Form detected
		
		if(formStart>=0 && formEnd>=0){
			
			int currentpos=documentMain.toLowerCase().indexOf("<input",formStart);
			
			while(currentpos>=formStart && currentpos<formEnd){

				String type=null, name=null, value=null;
				
				int closer=documentMain.toLowerCase().indexOf(">",currentpos);
				
				String[] sentence=documentMain.substring(currentpos, closer).split(" ");	// The input divided by the spaces
				
				for(String sentencePart : sentence){	// Read the parts of the input
					
					String partname=sentencePart.substring(0, sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=") : 0);
					
					String partvalue=sentencePart.substring(sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=")+1 : 0, sentencePart.length());
					
					
					switch(partname.toLowerCase()){
					
					case "type" : type=partvalue.replace("\"", ""); break;
					case "name" : name=partvalue.replace("\"", ""); break;
					case "value" : value=partvalue.replace("\"", ""); break;
					
					default:;
					
					}
					
				}
				
				if(type!=null && name!=null && value!=null)
					if(!type.toLowerCase().contains("checkbox"))	{ // To be sent
					
					if(output.length()>0) output.append("&");
					
					output.append(name + "=" + URLEncoder.encode(value, "UTF-8"));
					
				}
				
				// Prepare for next while loop
				currentpos=documentMain.toLowerCase().indexOf("<input",currentpos+1);
				
			}
			
			
		}
		
		if(output.length()>0) output.append("&");
		output.append("username=" + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8"));
		
		return output.toString();
		
	}
	
	public static String faiticLogin(String username, String password) throws Exception{
		
		String responseToLogin=requestDocument(urlMain,generatePostLogin(username, password));
		
		int errorToLoginIndex=responseToLogin.indexOf("<dd class=\"error message\">");
		
		// If there was an error
		if(errorToLoginIndex > 0){
		
			int firstLiError=responseToLogin.indexOf("<li>",errorToLoginIndex);
			int lastLiError=responseToLogin.indexOf("</li>",errorToLoginIndex);
		
			if(firstLiError>0 && lastLiError>firstLiError){
				
				logger.log(Logger.ERROR, " -- Error: " + responseToLogin.substring(firstLiError+4, lastLiError) + " -- ");
				return null;
				
			}
			
		}
		
		// No error, pass the document
		
		return responseToLogin;
		
		
	}
	

	public static String faiticLogout(String documentMain) throws Exception{

		StringBuffer output=new StringBuffer();
		
		int formStart=documentMain.toLowerCase().indexOf("<form action=\"/index.php/es/materias\" method=\"post\" id=\"login-form\"");
		
		int formEnd=documentMain.toLowerCase().indexOf("</form>", formStart);
		
		// Form detected
		
		if(formStart>=0 && formEnd>=0){
			
			int currentpos=documentMain.toLowerCase().indexOf("<input",formStart);
			
			while(currentpos>=formStart && currentpos<formEnd){

				String type=null, name=null, value=null;
				
				int closer=documentMain.toLowerCase().indexOf(">",currentpos);
				
				String[] sentence=documentMain.substring(currentpos, closer).split(" ");	// The input divided by the spaces
				
				for(String sentencePart : sentence){	// Read the parts of the input
					
					String partname=sentencePart.substring(0, sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=") : 0);
					
					String partvalue=sentencePart.substring(sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=")+1 : 0, sentencePart.length());
					
					
					switch(partname.toLowerCase()){
					
					case "type" : type=partvalue.replace("\"", ""); break;
					case "name" : name=partvalue.replace("\"", ""); break;
					case "value" : value=partvalue.replace("\"", ""); break;
					
					default:;
					
					}
					
				}
				
				if(type!=null && name!=null && value!=null)
					if(!type.toLowerCase().contains("checkbox"))	{ // To be sent
					
					if(output.length()>0) output.append("&");
					
					output.append(name + "=" + URLEncoder.encode(value, "UTF-8"));
					
				}
				
				// Prepare for next while loop
				currentpos=documentMain.toLowerCase().indexOf("<input",currentpos+1);
				
			}
			
			
		}
		
		return requestDocument(urlSubjects,output.toString());
		
	}
	
	
	public static ArrayList<String[]> faiticSubjects(String documentToCheck){	// 0 url 1 name
		
		ArrayList<String[]> subjectList=new ArrayList<String[]>();
		
		// Login was unsuccessful
		if(documentToCheck==null) return subjectList;
		
		// Login successful:
		
		int subjectIndex=documentToCheck.indexOf("<span class=\"asignatura\"");
		
		while(subjectIndex>=0){
			
			// Check subjects one by one
			
			int hrefIndex=documentToCheck.indexOf("<a href=\"", subjectIndex);
			int hrefURLCloserIndex=documentToCheck.indexOf("\"",hrefIndex+"<a href=\"".length());
			
			int hrefFirstTagCloserIndex=documentToCheck.indexOf(">",hrefURLCloserIndex);
			int hrefSecondTagOpenerIndex=documentToCheck.indexOf("<", hrefFirstTagCloserIndex);
			
			String[] subject=new String[2];
			
			subject[0]=documentToCheck.substring(hrefIndex+"<a href=\"".length(), hrefURLCloserIndex);
			subject[1]=documentToCheck.substring(hrefFirstTagCloserIndex+1, hrefSecondTagOpenerIndex).trim();
			
			subjectList.add(subject);
			
			subjectIndex=documentToCheck.indexOf("<span class=\"asignatura\"",subjectIndex+1);
			
		}
		
		return subjectList;
		
	}
	
	public static String[] goToSubject(String url) throws Exception{	// 0 is the url and 1 is the document itself
		
		String documentMain=requestDocument(url,"");
		
		StringBuffer output=new StringBuffer();
		
		int formStart=documentMain.toLowerCase().indexOf("<form name='frm'");
		
		int formEnd=documentMain.toLowerCase().indexOf("</form>", formStart);
		
		int actionStart=documentMain.indexOf("action='", formStart);
		int actionEnd=documentMain.indexOf("'", actionStart+"action='".length());
		
		String urlForAction=documentMain.substring(actionStart + "action='".length(), actionEnd);
		
		// Form detected
		
		if(formStart>=0 && formEnd>=0){
			
			int currentpos=documentMain.toLowerCase().indexOf("<input",formStart);
			
			while(currentpos>=formStart && currentpos<formEnd){

				String type=null, name=null, value=null;
				
				int closer=documentMain.toLowerCase().indexOf(">",currentpos);
				
				String[] sentence=documentMain.substring(currentpos, closer).split(" ");	// The input divided by the spaces
				
				for(String sentencePart : sentence){	// Read the parts of the input
					
					String partname=sentencePart.substring(0, sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=") : 0);
					
					String partvalue=sentencePart.substring(sentencePart.indexOf("=") >=0 ? sentencePart.indexOf("=")+1 : 0, sentencePart.length());
					
					
					switch(partname.toLowerCase()){
					
					case "type" : type=partvalue.replace("'", ""); break;
					case "name" : name=partvalue.replace("'", ""); break;
					case "value" : value=partvalue.replace("'", ""); break;
					
					default:;
					
					}
					
				}
				
				if(type!=null && name!=null && value!=null)
					if(!type.toLowerCase().contains("checkbox"))	{ // To be sent
					
					if(output.length()>0) output.append("&");
					
					output.append(name + "=" + URLEncoder.encode(value, "UTF-8"));
					
				}
				
				// Prepare for next while loop
				currentpos=documentMain.toLowerCase().indexOf("<input",currentpos+1);
				
			}
			
			
		}
		
		return new String[]{urlForAction,requestDocument(urlForAction,output.toString())};
		
		
	}
	
	public static final int CLAROLINE=0;
	public static final int MOODLE=1;
	public static final int MOODLE2=2;
	public static final int UNKNOWN=99;
	
	public static int subjectPlatformType(String url){
		
		if(url.toLowerCase().contains("/claroline/")){
			return CLAROLINE;
		}
		else if(url.toLowerCase().contains("/moodle") && !url.toLowerCase().contains("/moodle2_")){
			return MOODLE;
		}
		else if(url.toLowerCase().contains("/moodle2_")){
			return MOODLE2;
		}
		else{
			return UNKNOWN;
		}
		
	}
	
	public static void logoutSubject(String platformURL, String platformDocument, int platformType) throws Exception{
		
		if(platformType==CLAROLINE){
			
			String logoutURL=platformURL.substring(0, platformURL.lastIndexOf("?") >=0 ? platformURL.lastIndexOf("?") : platformURL.length()) + "?logout=true";
			
			requestDocument(logoutURL,"");
			
		}
		
		else if(platformType==MOODLE || platformType==MOODLE2){
			
			// More complicated :( pay attention because this is about to start...

			int endOfURLShouldStartWith= platformURL.indexOf("/", platformURL.indexOf("/moodle")+1);
			
			if(endOfURLShouldStartWith>=0){

				String logoutURLShouldStartWith=platformURL.substring(0, endOfURLShouldStartWith) + "/login/logout.php";
				// This is the url that should appear on the document, but with all the parameters given as GET
				
				// Let's look for this entry
				
				int hereIsTheLogoutURL=platformDocument.indexOf(logoutURLShouldStartWith);
				
				int hereEndsTheLogoutURL=platformDocument.indexOf("\"",hereIsTheLogoutURL);

				//System.out.println("\n\n" + logoutURLShouldStartWith + "\n\n");
				
				if(hereIsTheLogoutURL>=0 && hereEndsTheLogoutURL>hereIsTheLogoutURL){

					// Gotcha!
					
					requestDocument(platformDocument.substring(hereIsTheLogoutURL, hereEndsTheLogoutURL),"");
					
				}
				
			}
			
		}
		
	}
	
	public static ArrayList<String[]> listDocumentsClaroline(String platformURL) throws Exception{
		
		/*
		 * 0 -> Path (incl. filename)
		 * 1 -> URL to file
		 */
		
		ArrayList<String[]> list=new ArrayList<String[]>();
		
		int untilWhenUrlToUse= platformURL.indexOf("/", platformURL.indexOf("/claroline")+1);
		
		if(untilWhenUrlToUse>=0){
			
			String urlBase = platformURL.substring(0, untilWhenUrlToUse);
			String urlToUse =  urlBase + "/document/document.php";
			list=listDocumentsClarolineInternal(urlToUse,list, urlBase);	// Recursive
			
		}
		
		return list;
		
	}
	
	private static ArrayList<String[]> listDocumentsClarolineInternal(String urlToAnalyse, ArrayList<String[]> list, String urlBase) throws Exception{

		String document;
		
		try{
			
			document=requestDocument(urlToAnalyse,"");
			
		} catch(Exception ex){
			
			return list;
			
		}
		
		if(!urlToAnalyse.equals(lastRequestedURL)) return list;		// If the page redirected us
		
		// Check for documents...
		
		int dirStart=document.indexOf("<a class=\" item");
		
		int dirEnd=document.lastIndexOf("End of Claroline Body");
		
		if(dirStart>=0 && dirEnd>dirStart){
			
			String documentToAnalyse=document.substring(dirStart, dirEnd);
			
			// First check for files

			int ocurrence=documentToAnalyse.indexOf("goto/index.php");
			
			while(ocurrence>=0){
				
				int endOfOcurrence=documentToAnalyse.indexOf("\"", ocurrence+1);
				
				if(endOfOcurrence>ocurrence){
					
					String urlGot=urlBase + "/document/" + documentToAnalyse.substring(ocurrence, endOfOcurrence).replace("&amp;", "&");
					
					String pathForFile=urlGot.substring((urlBase + "/document/goto/index.php/").length(), urlGot.lastIndexOf("?") >=0 ? urlGot.lastIndexOf("?") : urlGot.length());
					
					list.add(new String[]{ URLDecoder.decode(pathForFile, "iso-8859-1") , urlGot });
					
				}
				
				ocurrence=documentToAnalyse.indexOf("goto/index.php", ocurrence+1);
				
			}
			
			
			// Now for directories
			
			ocurrence=documentToAnalyse.indexOf("/document/document.php?cmd=exChDir");
			
			while(ocurrence>=0){
				
				int endOfOcurrence=documentToAnalyse.indexOf("\"", ocurrence+1);
				
				if(endOfOcurrence>ocurrence){

					String urlGot=urlBase + documentToAnalyse.substring(ocurrence, endOfOcurrence).replace("&amp;", "&");
					
					listDocumentsClarolineInternal(urlGot, list, urlBase);
					
				}
				
				ocurrence=documentToAnalyse.indexOf("/document/document.php?cmd=exChDir", ocurrence+1);
				
			}
			
		}
		
		
		
		return list;
		
	}
	
	public static ArrayList<String[]> listDocumentsMoodle(String platformURL) throws Exception{
		
		/*
		 * 0 -> Path (incl. filename)
		 * 1 -> URL to file
		 */
		
		ArrayList<String[]> list=new ArrayList<String[]>();
		
		int untilWhenUrlToUse= platformURL.indexOf("/", platformURL.indexOf("/moodle")+1);
		
		if(untilWhenUrlToUse>=0){
			
			String urlBase = platformURL.substring(0, untilWhenUrlToUse);
			String urlGetMethod=platformURL.indexOf("?") >= 0 ? platformURL.substring(platformURL.indexOf("?") + 1, platformURL.length()) : "";
			String urlForResources= urlBase + "/mod/resource/index.php" + (urlGetMethod.length()>0 ? "?" + urlGetMethod : "");
			
			String resourcePage=requestDocument(urlForResources, "");
			
			// Resources page, get the list of lists. Yes. The list of lists. Because there are several lists :3
			
			int bodyStart=resourcePage.indexOf("<!-- END OF HEADER -->");
			
			int bodyEnd=resourcePage.indexOf("<!-- START OF FOOTER -->", bodyStart);
			
			if(bodyStart >=0 && bodyEnd > bodyStart){
				
				String whereToSearchForLists=resourcePage.substring(bodyStart, bodyEnd);
				
				//System.out.println(whereToSearchForLists);
				
				int listURLStart=whereToSearchForLists.indexOf("view.php?");
				int listURLEnd=whereToSearchForLists.indexOf("\"", listURLStart);
				
				while(listURLStart>=0 && listURLStart<listURLEnd){
					
					String urlList=urlBase + "/mod/resource/" + whereToSearchForLists.substring(listURLStart, listURLEnd);
					urlList=urlList.replace("&amp;", "&");
					
					int listNameStart=whereToSearchForLists.indexOf(">", listURLStart);
					int listNameEnd=whereToSearchForLists.indexOf("</a>", listNameStart);
					
					if(listNameStart<0 || listNameEnd<=listNameStart) return list;
					
					String listName=whereToSearchForLists.substring(listNameStart+1, listNameEnd);
					
					listDocumentsMoodleInternal(urlList, list, urlBase, listName);
					
					// For next loop
					
					listURLStart=whereToSearchForLists.indexOf("view.php?", listURLEnd);
					listURLEnd=whereToSearchForLists.indexOf("\"", listURLStart);
					
				}
				
			}
			
			//String urlToUse =  urlBase + "/document/document.php";
			//list=listDocumentsClarolineInternal(urlToUse,list, urlBase);	// Recursive
			
		}
		
		return list;
		
	}
	
	private static ArrayList<String[]> listDocumentsMoodleInternal(String urlToUse, ArrayList<String[]> list, String urlBase, String listName) throws Exception{
		
		//System.out.println("---Accessed---");

		String resourcePage;
		
		try{
			
			resourcePage=requestDocument(urlToUse, "");
			
		} catch(Exception ex){
			
			return list;
			
		}
		
		if(!urlToUse.equals(lastRequestedURL)) return list;		// If the page redirected us
		
		// The list of files from this resource
		
		int bodyStart=resourcePage.indexOf("<!-- END OF HEADER -->");
		
		int bodyEnd=resourcePage.indexOf("<!-- START OF FOOTER -->", bodyStart);
		
		if(bodyStart >=0 && bodyEnd > bodyStart){
			
			String whereToSearch=resourcePage.substring(bodyStart, bodyEnd);
			
			//System.out.println(whereToSearch);
			
			// First files
			
			int URLStart=whereToSearch.indexOf(urlBase + "/file.php/");
			int URLEnd=whereToSearch.indexOf("\"", URLStart);
			
			while(URLStart>=0 && URLStart<URLEnd){
				
				String urlToFile=whereToSearch.substring(URLStart, URLEnd);
				urlToFile=urlToFile.replace("&amp;", "&");
				
				int filePathStart=urlToFile.indexOf("/", (urlBase + "/file.php/").length()+1);
				
				String filePath=urlToFile.substring(filePathStart, urlToFile.length());
				
				list.add(new String[]{URLDecoder.decode(filePath, "iso-8859-1"),urlToFile});	// Added to list
				
				// For next loop
				
				URLStart=whereToSearch.indexOf(urlBase + "/file.php/", URLEnd);
				URLEnd=whereToSearch.indexOf("\"",URLStart);
				
			}
			
			// Then directories
			
			URLStart=whereToSearch.indexOf("view.php?");
			URLEnd=whereToSearch.indexOf("\"", URLStart);
			
			while(URLStart>=0 && URLStart<URLEnd){
				
				String urlList=urlBase + "/mod/resource/" + whereToSearch.substring(URLStart, URLEnd);
				urlList=urlList.replace("&amp;", "&");
				
				listDocumentsMoodleInternal(urlList, list, urlBase, listName);
				
				// For next loop
				
				URLStart=whereToSearch.indexOf("view.php?", URLEnd);
				URLEnd=whereToSearch.indexOf("\"", URLStart);
				
			}
			
		}
		
		
		return list;
		
	}
	
	

	public static ArrayList<String[]> listDocumentsMoodle2(String platformURL) throws Exception{
		
		/*
		 * 0 -> Path (incl. filename)
		 * 1 -> URL to file
		 */
		
		ArrayList<String[]> list=new ArrayList<String[]>();
		
		int untilWhenUrlToUse= platformURL.indexOf("/", platformURL.indexOf("/moodle")+1);
		
		if(untilWhenUrlToUse>=0){
			
			String urlBase = platformURL.substring(0, untilWhenUrlToUse);
			String urlGetMethod=platformURL.indexOf("?") >= 0 ? platformURL.substring(platformURL.indexOf("?") + 1, platformURL.length()) : "";
			String urlForResources= urlBase + "/mod/resource/index.php" + (urlGetMethod.length()>0 ? "?" + urlGetMethod : "");
			
			listDocumentsMoodle2Internal(urlForResources, list, urlBase, "");
			
		}
		
		return list;
		
	}
	

	private static ArrayList<String[]> listDocumentsMoodle2Internal(String urlToUse, ArrayList<String[]> list, String urlBase, String folder) throws Exception{
		
		//System.out.println("---Accessed---");

		String resourcePage;
		
		try{
			
			resourcePage=requestDocument(urlToUse, "");
			
		} catch(Exception ex){
			
			return list;
			
		}
		
		if(!urlToUse.equals(lastRequestedURL)) return list;		// If the page redirected us
		
		// The list of files from this resource
		
		int bodyStart=resourcePage.indexOf("<div id=\"page-content\"");
		
		int bodyEnd=resourcePage.indexOf("</section>", bodyStart);
		
		if(bodyStart >=0 && bodyEnd > bodyStart){
			
			String whereToSearch=resourcePage.substring(bodyStart, bodyEnd);
			
			int URLStart=whereToSearch.indexOf("view.php?");
			int URLEnd=whereToSearch.indexOf("\"", URLStart);
			
			while(URLStart>=0 && URLStart<URLEnd){
				
				String urlList=urlBase + "/mod/resource/" + whereToSearch.substring(URLStart, URLEnd);
				urlList=urlList.replace("&amp;", "&");
				
				// We have got the url, but we don't know if it's a folder or not, let's check it
				
				int indeximg=whereToSearch.indexOf("<img src=", URLEnd);
				int endofimg=whereToSearch.indexOf(">", indeximg);
				
				int endofa=whereToSearch.indexOf("<", endofimg);
				
				int folderindex=whereToSearch.indexOf("folder-24",indeximg);
				
				String filename=endofimg>=0 && endofa>endofimg ? whereToSearch.substring(endofimg+1, endofa).trim() : "undefined";
				
				if(folderindex>=0 && folderindex<endofimg){
					
					// Folder, recursive search
					
					listDocumentsMoodle2Internal(urlList, list, urlBase, folder + "/" + filename);
					
				} else{
					
					// Document, let's get the real name
					
					String realurl = getRedirectedURL(urlList, "");
					String realname=filename;	// By now
					
					if(realurl!=null){
						
						// Redirected, get the real name
						
						int questionMarkIndex=realurl.indexOf("?");
						int lastDivider=realurl.substring(0, questionMarkIndex >=0 ? questionMarkIndex : realurl.length()).lastIndexOf("/");	// No error because it starts at 0
						
						if(lastDivider>=0){
							
							// Got a name
							
							realname=URLDecoder.decode(realurl.substring(lastDivider+1, questionMarkIndex>=0 ? questionMarkIndex : realurl.length()),"UTF-8");
							
						}
						
						
					}
					
					list.add(new String[]{folder + "/" + realname, urlList});

				}
				
				// For next loop
				
				URLStart=whereToSearch.indexOf("view.php?", URLEnd);
				URLEnd=whereToSearch.indexOf("\"", URLStart);
				
			}
			
		}
		
		
		return list;
		
	}

}

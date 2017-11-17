package Rep;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Future;

import org.json.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Testing class to hold info about a school and run several fucntions for that school
 * such as report gathering, etc
 * @author jchavis
 *
 */
public class APIClass {

	private String key;
	private String name;
	private String token;
	private Vector<User> users;
	private User admin;
	private String location = "";

	/**
	 * This class represents a school and holds all info and runs functions.
	 * @param nameOfSchool - Name of the school
	 * @param apiKey - API key for the school
	 * @param adminUser - User that holds Admin user
	 */
	public APIClass(String nameOfSchool, String apiKey, User adminUser){
		name = nameOfSchool;
		key = apiKey;
		users = new Vector<>();
		admin = adminUser;
		users.add(admin);//adds given admin to the user's list.
		makeToken(); //runs the process of creating the token
	}

	/**
	 * determines if a token is already saved on a file. If so, it reads it, if not, makes it
	 */
	private void makeToken(){
		try {
			String fileName = name + admin.getUserName() +"-token.txt";
			File file = new File(fileName);//load the token file
			ZonedDateTime fileTime = null;
			if(file.exists()){
				Path p = Paths.get(fileName);
				BasicFileAttributes view = Files.getFileAttributeView(p, BasicFileAttributeView.class)
						.readAttributes();
				System.out.println(view.creationTime() + "\t" + view.lastModifiedTime());
				fileTime = ZonedDateTime.parse(view.lastModifiedTime().toString());

				//if the file is more than 59 minutes old, delete it and start again
				System.out.println(ChronoUnit.MINUTES.between(fileTime, ZonedDateTime.now()));
				if(ChronoUnit.MINUTES.between(fileTime, ZonedDateTime.now()) >= 59){
					Files.delete(p);
				}
			}

			//if the file still exists at this point, it's less than 60 min old.
			if(file.exists()){
				Path p = Paths.get(fileName);
				BasicFileAttributes view = Files.getFileAttributeView(p, BasicFileAttributeView.class)
						.readAttributes();


				Scanner in = new Scanner(file);
				token = in.nextLine();
				in.close();

				if(token.equals("") || token == null){//check if token is there
					buildToken();//if the token was not found, make one
				} else {//found and read in the token
					System.out.println("using stored token");
				}
			} else {
				buildToken();//the token file does not exist, go ahead and make it
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * fetches the token and writes it to a file.
	 */
	private void buildToken(){
		String loginURL = "https://secure3.saashr.com:443/ta/rest/v1/login";
		String tToken = "";

		JSONObject jo = new JSONObject();
		jo.put("username", admin.getUserName());
		jo.put("password", admin.getPassword());
		jo.put("company", name);
		JSONObject body = new JSONObject();
		body.put("credentials", jo);//place the jo json object inside of the body json object.

		try {
			HttpResponse<JsonNode> jr = Unirest.post(loginURL)
					.header("accept", "application/json")
					.header("Content-Type", "application/json")
					.header("Cache-Control", "no-cache")
					.header("x-forward-proto", "https")
					.header("Api-Key", key)
					.body(body)
					.asJson();

			if(jr.getStatus() != 200){
				System.err.println("Error: Status Code " + jr.getStatus() + " - " + jr.getStatusText());
				System.exit(0);
			} else {
				tToken = jr.getBody().getObject().getString("token");
			}

			token = tToken;
			System.out.println("Creating a new token");
			PrintWriter writer = new PrintWriter(name + admin.getUserName() +"-token.txt", "UTF-8");
			writer.println(token);
			writer.close();
			System.out.println("New Token Saved");

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			token = "";
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Fetches the global report for the given report ID and returns the report as 
	 * one long string. If you want some sort parsing, maybe you should ask a fancy
	 * programmer, not a state school grad.
	 */
	public String getGlobalReport(String id){
		String url = "https://secure3.saashr.com:443/ta/rest/v1/report/global/";
		String reportText = "";

		try {
			reportText = Unirest.get(url + id)
					.header("accept", "text/csv")
					.header("Cache-Control", "no-cache")
					.header("Authentication", "Bearer " + token)
					.asString().getBody();
		} catch (Exception e){
			e.printStackTrace();
		}
		return reportText;
	}

	/**
	 * Function returns a saved report. Note, saved reports are not global reports and they
	 * are not exports. See the getExportData() and getGlobalReport()
	 * @param id - system id of the saved report
	 * @return string of the report body
	 */
	public String getSavedReport(String id){
		String url = "https://secure3.saashr.com:443/ta/rest/v1/report/saved/";
		HttpResponse<String> reportText = null;

		try {
			reportText = Unirest.get(url + id)
					.header("accept", "text/csv")
					.header("Cache-Control", "no-cache")
					.header("Authentication", "Bearer " + token)
					.asString();
		} catch (Exception e){
			e.printStackTrace();
		}
		return reportText.getBody();
	}


	/**
	 * This function fetches a data export from the API in an async manner that
	 * waits for a response. Once the response is received, the response header is 
	 * read for a location that the file can be fetched. This is fethched in a seperate
	 * sync HTTP get call.
	 * @param id - the System ID of the system export
	 * @return String of the csv contents.
	 */
	public String getExportData(String id){
		String url = "https://secure3.saashr.com:443/ta/rest/v1/export/"; // the base export url for the api
		Future<HttpResponse<JsonNode>> future = null; // this is the call back future
		String returnText = ""; //holds the text to be returned

		try {
			//start the async fetch
			System.out.println("trying " + url + id);
			future = Unirest.get(url + id)
					.header("accept", "application/json")
					.header("Authentication", "Bearer " + token)
					.asJsonAsync(new Callback<JsonNode>() {

						//call failed, timed out, etc
						public void failed(UnirestException e) {
							System.out.println("The request has failed");
						}

						//once the call succeeds, 
						public void completed(HttpResponse<JsonNode> response) {
							System.out.println("The request has finished with code: " + response.getStatus());
							//int code = response.getStatus();
							Map<String, List<String>> headers = response.getHeaders();//read headers
							//JsonNode body = response.getBody();
							//InputStream rawBody = response.getRawBody();
							//System.out.println(response.getBody().toString());
							location = headers.get("Location").get(0);//read location header (assume 1)
							System.out.println("Fetching file at: " + location);
						}

						//if the future is cancelled
						public void cancelled() {
							System.out.println("The request has been cancelled");
						}
					});

			//this is not really needed, just there so the console doesn't go dead
			long counter = 0;
			while(!future.isDone()){
				Thread.sleep(200);
				counter += 200;
				System.out.println("waiting..." + counter);
			}

			
			location = "https://secure3.saashr.com:443/ta/rest/v1" + location;//file url
			System.out.println(location); // look ma! I got the file location
			
			HttpResponse<String> response = null; //holds the file fetch
			boolean ready = false;
			int waitTime = 500;

			while(!ready){

				response= Unirest.get(location) //got get it
						.header("accept", "application/json")
						.header("Authentication", "Bearer " + token)
						.asString();
				System.out.println("status code for file: " + response.getStatus());
				if(response.getStatus() == 202 || response.getBody().equals("{\"status\":\"running\"}")){
					waitTime = (int) (waitTime*1.5);

					if(waitTime >= 6000) {
						System.err.println("Waiting over a minute, quiting");
						System.exit(1);
					}
					System.out.println("Waiting " + waitTime + "(ms)");
					Thread.sleep(waitTime);
				} else {
					ready = true;
					System.out.println("File is ready");
				}
			}

			if(response.getStatus() != 200){ //if the future returned and the file is still not there, throw err
				System.err.println("Status Code of " + response.getStatus() + " returned");
			}

			returnText = response.getBody().replaceAll("\u00A0", ""); // this could be error response or full csv
			System.out.println(returnText); // show me what I won
		} catch (Exception e){
			e.printStackTrace();
		}
		return returnText;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getToken() {
		return token;
	}

	public void setToken() {
		buildToken();
	}

	public Vector<User> getUsers() {
		return users;
	}

	public void setUsers(Vector<User> users) {
		this.users = users;
	}

	public User getAdmin() {
		return admin;
	}

	public void setAdmin(User admin) {
		this.admin = admin;
		users.add(admin);
	}
}

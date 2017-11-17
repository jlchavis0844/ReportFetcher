package Rep;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

	static String APIKey = null;
	static String APIPass = null;
	static String APIUser = null;
	static String APISchool = null;
	static String reportNum = null;
	static String workDir = "";
	static int type = -1;
	final static int EXPORT = 0;
	final static int SAVED = 1;
	final static int GLOBAL = 2;

	public static void main(String[] args) {
		if(args.length != 7){
			System.err.println("Incorrect args: " + args.length);
			for(String s: args)
				System.out.println(s);
			System.exit(-1);
		} else {
			APIUser = args[0].toString().trim();
			APIPass = args[1].toString().trim();
			APISchool = args[2].toString().trim();
			APIKey = args[3].toString().trim();
			reportNum = args[4].toString().trim();
			workDir = args[5].toString().trim();
			type = Integer.valueOf(args[6].toString().trim());

			if(type < EXPORT || type > GLOBAL){
				System.err.println("Incorrect type: " + args.length);
				System.exit(-1);
			}
		}

		User restUser = new User(APIUser, APIPass, APISchool);
		APIClass apiClass = new APIClass(APISchool, APIKey, restUser);

		//System.out.println(apiClass.getToken());
		String output = "";
		switch (type) {
			case EXPORT:
				System.out.println("Fetching ...");
				output = apiClass.getExportData(reportNum);
				break;
			case SAVED:
				System.out.println("Fetching ...");
				output = apiClass.getSavedReport(reportNum);
				break;
			case GLOBAL:
				System.out.println("Fetching ...");
				output = apiClass.getGlobalReport(reportNum);
				break;
			default:
				System.err.println("Incorrect type: " + args.length);
				System.exit(-1);
				break;
		}

		//String output = "test";
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String stamp = now.format(formatter);
		String fname = APISchool + "_EDI_" + stamp + ".csv";
		BufferedWriter bw = null;
		System.out.println(fname);
		File file = null;

		try{
			file = new File(workDir + "\\Reports\\" + fname);
			file.getParentFile().mkdirs();
			bw = new BufferedWriter(new FileWriter(file));
			bw.write(output);
			Files.copy(file.toPath(),new File(workDir + "\\currentRep.csv").toPath(), 
					StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Saving to: " + file.getAbsolutePath());

		} catch(IOException e){
			e.printStackTrace();
		} finally{
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		System.out.println(APIUser);
		System.out.println(APIPass);
		System.out.println(APISchool);
		System.out.println(APIKey);
		System.out.println(reportNum);
		System.out.println(workDir);
		System.out.println(type);
		System.exit(0);
	}
}

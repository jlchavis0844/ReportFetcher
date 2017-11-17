package Rep;

public class User {
	private String userName;
	private String password;
	private String company;
	
	/**
	 * Constructor for simple user class that holds username, password, and school
	 * @param uName - user name
	 * @param pass - password
	 * @param school - school abrv
	 */
	public User(String uName, String pass, String school){
		userName = uName;
		password = pass;
		company = school;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

}

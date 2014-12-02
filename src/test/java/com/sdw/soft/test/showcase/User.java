package com.sdw.soft.test.showcase;
/**
 * @author Sonicery_D
 * @date 2014年12月2日
 * @version 1.0.0
 * @description
 **/
public class User {

	private String id;
	private String username;
	private String password;
	private int age;
	private String address;
	private String create_date;
	
	public User(String id, String username, String password, int age,
			String address, String create_date) {
		super();
		this.id = id;
		this.username = username;
		this.password = password;
		this.age = age;
		this.address = address;
		this.create_date = create_date;
	}
	public User(String username, String password, int age, String address,
			String create_date) {
		super();
		this.username = username;
		this.password = password;
		this.age = age;
		this.address = address;
		this.create_date = create_date;
	}
	public User() {
		super();
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCreate_date() {
		return create_date;
	}
	public void setCreate_date(String create_date) {
		this.create_date = create_date;
	}
}

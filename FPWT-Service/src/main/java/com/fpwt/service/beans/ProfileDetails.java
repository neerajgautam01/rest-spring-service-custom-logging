package com.fpwt.service.beans;

public class ProfileDetails {
	private String name;
	private Integer age;
	private String occupation;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return this.age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getOccupation() {
		return this.occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	@Override
	public String toString() {
		return "ProfileDetails [name=" + name + ", age=" + age + ", occupation=" + occupation + "]";
	}

}
package com.wushuangtech.library;

public class Conference {

	private long id;
	private int userRole;

	Conference(long id, int userRole) {
		this.id = id;
		this.userRole = userRole;
	}

	public long getId() {
		return this.id;
	}
	public int getUserRole() {return this.userRole;};
}

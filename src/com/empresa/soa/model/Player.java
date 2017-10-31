package com.empresa.soa.model;

import java.io.Serializable;

public class Player implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4491906615982489993L;
	
	private String name;
	private String nickname;

	public Player() {
	}

	public Player(String name, String nickname) {
		setName(name);
		setNickname(nickname);
	}

	public void setName(String name) { 
		this.name = name; 
	}
	
	public String getName() { 
		return name; 
	}
	
	public void setNickname(String nickname) { 
		this.nickname = nickname; 
	}
	
	public String getNickname() { 
		return nickname; 
	}
}
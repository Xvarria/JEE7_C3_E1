package com.empresa.soa.rest;

import javax.xml.ws.Endpoint;

public class ServerPublisher {

	//Clase para publicar un webservice
	public static void main(String[] args) {
		int port = 8888;
		String server = "localhost";
		String url = "http://" + server + ":" + port + "/teams";
		System.out.println("Publishing Teams restfully on port " + port);
		Endpoint.publish(url, new RestfulTeam(true));

		String url2 = "http://" + server + ":" + port + "/ts";
		System.out.println("Publishing Time restfully on port " + port);
		Endpoint.publish(url2, new TimeServer());
	}

}

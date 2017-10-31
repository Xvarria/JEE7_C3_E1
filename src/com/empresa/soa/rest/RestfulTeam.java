package com.empresa.soa.rest;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.http.HTTPException;

import com.empresa.soa.model.Player;
import com.empresa.soa.model.Team;

// Clase que implementa la interface
// tanto para webservices SOAP o REST
@WebServiceProvider

// Clase que accesa el mensaje completo (header/body) o el
// PAYLOAD (body únicamente)
@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)

// Implementa el transporte HTTP, en vez de SOAP
@BindingType(value = HTTPBinding.HTTP_BINDING)
public class RestfulTeam implements Provider<Source> {
	@Resource
	protected WebServiceContext wsCtx;

	private Map<String, Team> teamMap; // for easy lookups
	private List<Team> teams;
	// serialized/deserialized
	private byte[] teamBytes;

	// from the persistence file
	private static final String fileName = "teams.ser";

	/**
	 * Constructor
	 * @param file true lee desde el archivo
	 */
	public RestfulTeam(Boolean file) {
		if (file) {
			// read the raw bytes from teams.ser
			readTeamsFromFile();
			// deserialize to a List<Team>
			deserialize();
		}
	}

	/**
	 * Método implementado para manejo de solicitudes
	 * OBLIGATORIO
	 */
	@Override
	public Source invoke(Source request) {
		if (wsCtx == null) {
			throw new RuntimeException("Dependence Inyection (DI) falló wsCtx.");
		}
		// Toma el mensaje del contexto y extrae el verbo.
		MessageContext msg_ctx = wsCtx.getMessageContext();
		String http_verb = (String) msg_ctx.get(MessageContext.HTTP_REQUEST_METHOD);
		http_verb = http_verb.trim().toUpperCase();

		// GET
		if (http_verb.equals("GET")) {
			return doGet(msg_ctx);
		} else {
			throw new HTTPException(405); // 405 ==> método no permitido
		}
	}

	/**
	 * 
	 * @param msgCtx Contexto del servidor web
	 * @return response
	 */
	private Source doGet(MessageContext msgCtx) {
		// Parse the query string.
		String queryString = (String) msgCtx.get(MessageContext.QUERY_STRING);
		// Get all teams.
		if (queryString == null)
			return new StreamSource(new ByteArrayInputStream(teamBytes));
		// Get a named team.
		else {
			String name = getValueFromQueryString("name", queryString);
			// Check if named team exists.
			Team team = teamMap.get(name);
			if (team == null) {
				throw new HTTPException(404); // not found
			}
			// Otherwise, generate XML and return.
			ByteArrayInputStream stream = encodeToStream(team);
			return new StreamSource(stream);
		}
	}

	/**
	 * 
	 * @param obj para codificar respuesta
	 * @return objeto codificado
	 */
	private ByteArrayInputStream encodeToStream(Object obj) {
		// Serialize object to XML and return
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLEncoder enc = new XMLEncoder(stream);
		enc.writeObject(obj);
		enc.close();
		return new ByteArrayInputStream(stream.toByteArray());
	}

	/**
	 * 
	 * @param key busqueda
	 * @param qs query string del GET
	 * @return parametro de busqueda
	 */
	private String getValueFromQueryString(String key, String qs) {
		String[] parts = qs.split("=");
		// Check if query string has form: name=<team name>
		if (!parts[0].equalsIgnoreCase(key))
			throw new HTTPException(400); // bad request
		return parts[1].trim();
	}

	/**
	 * Lee el archivo de equipos
	 */
	private void readTeamsFromFile() {
		try {
			String path = getFilePath();
			int len = (int) new File(path).length();
			teamBytes = new byte[len];
			FileInputStream file = new FileInputStream(path);
			file.read(teamBytes);
			file.close();
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * Convierte el archivo en objetos
	 */
	@SuppressWarnings("unchecked")
	private void deserialize() {
		// Deserialize the bytes into a list of teams
		XMLDecoder dec = new XMLDecoder(new ByteArrayInputStream(teamBytes));
		teams = (List<Team>) dec.readObject();
		dec.close();
		// Create a map for quick lookups of teams.
		teamMap = Collections.synchronizedMap(new HashMap<String, Team>());
		for (Team team : teams) {
			teamMap.put(team.getName(), team);
		}

	}

	/**
	 * 
	 * @return nombre del archivo donde estan almacenados
	 *         los equipos
	 */
	private String getFilePath() {
		String cwd = System.getProperty("user.dir");
		String sep = System.getProperty("file.separator");
		return cwd + sep + "team" + sep + fileName;
	}

	/**
	 * Método de prueba, para crear el archivo de los equipos
	 * @param args
	 */
	public static void main(String args[]) {
		RestfulTeam rest = new RestfulTeam(false);
		System.out.println(rest.getFilePath());

		Player p1 = new Player("uno1", "UNO");
		Player p2 = new Player("dos2", "DOS");
		List<Player> lista = new ArrayList<Player>();
		lista.add(p1);
		lista.add(p2);
		Team team = new Team("Numeros", lista);

		Player p11 = new Player("a1", "A");
		Player p21 = new Player("b2", "B");
		Player p31 = new Player("c3", "C");
		List<Player> lista1 = new ArrayList<Player>();
		lista1.add(p11);
		lista1.add(p21);
		lista1.add(p31);
		Team team1 = new Team("Letras", lista1);

		List<Team> equipos = new ArrayList<Team>();
		equipos.add(team);
		equipos.add(team1);

		String filename = "teams.ser";
		try {
		// ... and serialize it via XMLEncoder to file testbeanlist.xml
		final XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(filename)));
		encoder.writeObject(equipos);
		encoder.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * // Escribe el archivo con los equipos String filename = "teams.ser";
		 * // save the object to file FileOutputStream fos = null;
		 * ObjectOutputStream out = null; try { fos = new
		 * FileOutputStream(filename); out = new ObjectOutputStream(fos);
		 * out.writeObject(equipos); out.close(); } catch (Exception ex) {
		 * ex.printStackTrace(); }
		 */

		/*
		 * // Lee el archivo de los equipos FileInputStream fis = null;
		 * ObjectInputStream in = null; try { fis = new
		 * FileInputStream(filename); in = new ObjectInputStream(fis);
		 * List<Team> teams = (List<Team>) in.readObject(); in.close(); } catch
		 * (Exception ex) { ex.printStackTrace(); } System.out.println(teams);
		 */

	}

}

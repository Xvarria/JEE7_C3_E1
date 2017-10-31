package com.empresa.soa.rest;

import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

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

// Clase que implementa la interface
// tanto para webservices SOAP o REST
@WebServiceProvider

// Clase que accesa el mensaje completo (header/body) o el
// PAYLOAD (body únicamente)
@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)

// Implementa el transporte HTTP, en vez de SOAP
@BindingType(value = HTTPBinding.HTTP_BINDING)
public class TimeServer implements Provider<Source> {
	@Resource
	protected WebServiceContext wsCtx;

	/**
	 * Constructor
	 * @param file true lee desde el archivo
	 */
	public TimeServer() {

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
		StreamSource result = new StreamSource(new Date().toString());
		ByteArrayInputStream stream = encodeToStream(result);
		
		if (queryString == null)
			return new StreamSource(stream);
		// Get a named team.
		else {
			String time = getValueFromQueryString("time", queryString);
			if (time == null) {
				throw new HTTPException(404); // not found
			} 
			if (time.equals("string")) {
				return new StreamSource(stream);
			}
			if (time.equals("long")) {
				return new StreamSource( encodeToStream(new Date().getTime()));
			}
			throw new HTTPException(404); // not found
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
		return parts.length > 1 ? parts[1].trim() : null;
	}

}

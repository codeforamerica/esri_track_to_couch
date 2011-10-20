// FOOD TRUCKS = CLIENT ID 36155

import com.esri.tracking.connector.Connector;
import com.esri.tracking.connector.ConnectorAdapter;
import com.esri.tracking.connector.Constants;
import com.esri.tracking.connector.TrackingConnectionException;
import com.esri.tracking.connector.events.AuthenticateEvent;
import com.esri.tracking.connector.events.ConnectEvent;
import com.esri.tracking.connector.events.ServiceDefinitionsEvent;
import com.esri.tracking.connector.enums.GeometryType;
import com.esri.tracking.connector.geometry.TmsPoint;
import com.esri.tracking.connector.Observation;
import com.esri.tracking.connector.events.ObservationEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONWriter;

import org.apache.commons.codec.binary.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;

@SuppressWarnings("serial")
public class FollowService implements Runnable
{
	Connector tsconnector = null;
	MyAdapter adapter = new MyAdapter();

	String source_host;
	int source_port;
	String destination_uri;
	String destination_username;
	String destination_password;

	public static void main(String[] args){
		if (args.length != 5)
		{
			System.out.println("usage: FollowService <tracking server host> <tracking server port> <destination URI> <destination username> <destination password>");
			return;
		}
		FollowService fs = new FollowService(args[0],Integer.parseInt(args[1]),args[2],args[3],args[4]);
		fs.init();
	}

	public FollowService(String _source_host, int _source_port, String _dest_uri, String _dest_username, String _dest_password){
		source_host = _source_host;
		source_port = _source_port;
		destination_uri = _dest_uri;
		destination_username = _dest_username;
		destination_password = _dest_password;
	}

	public void init()
	{
		System.out.println("Connecting...");
		Thread thread = new Thread(this);
		thread.start();
	}

	/**
	 * Main method, initializes the Connector and connects to Tracking Server.
	 * Reports when message formats are received and if there are any errors.
	 * Parses the message formats XML so that it can subscribe to all
	 * Tracking Services.
	 */
	public void run()
	{
		tsconnector = new Connector(adapter);

		// connect to tracking server
		try {
			tsconnector.connectAsync(source_host, source_port);
		}
		catch (TrackingConnectionException tex)
		{
			System.out.println(tex.getMessage());
		}
	}

	private class MyAdapter extends ConnectorAdapter
	{

		@Override
		public void AuthenticateResponse(AuthenticateEvent e)
		{
			try {
				tsconnector.getServiceDefinitionAsync();
			} catch (Exception e1)
			{
				System.out.println(e1.toString());
			}
		}

		@Override
		public void ConnectResponse(ConnectEvent e)
		{
			try {
				tsconnector.authenticateAsync("", "");
			} catch (Exception e1)
			{
				System.out.println( e1.toString() );
			}
		}

		@Override
		public void ObservationReceived(ObservationEvent e)
		{
			Observation observation = e.getObservation();

			//create the JSON representation of this event
			JSONObject document = new JSONObject();
			for (String column : observation.getMessageDefinition().getFieldNameList())
			{
				try {
					if (column.length() > 0 && column != "LOCATION") {
						document.put(column, observation.getAttribute(column));
					}
				} catch(Exception err) {
				}
			}
			String jsondata = document.toString();
			//encode the jsondata in UTF-8
			StringEntity se = null;
			try {
				se = new StringEntity(jsondata, "UTF-8");
			} catch (UnsupportedEncodingException err) {
				System.out.println( "Encoding Error: " + err);
			}

			//post it to our destination couch
				//part 1: get a post object
			HttpPost httpost = new HttpPost(destination_uri);
				//part 2: create our authentication
			String userpass = destination_username+":"+destination_password;
			String encoding = new sun.misc.BASE64Encoder().encode(userpass.getBytes());
			httpost.setHeader("Authorization", "Basic " + encoding);
				//part 3: set headers & body
			httpost.setEntity(se);
			httpost.setHeader("Accept", "application/json");
			httpost.setHeader("Content-type", "application/json");
				//part 4: execute the post
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpResponse response = null;
			String ret = null;
			try {
				response = httpclient.execute(httpost, localContext);
				if (response != null) {
					ret = EntityUtils.toString(response.getEntity());
				}
			} catch (Exception err) {
				System.out.println("HTTP Error: " + err);
			}
				//part 5: we're done. print our result.
			System.out.println(ret);
		}

		@Override
		public void ServiceDefinitionsReponse(ServiceDefinitionsEvent e)
		{
			// subscribe to all services
			System.out.println("Subscribing to services: \n");
			String[] serviceNames = tsconnector.getServiceNames();
			for (int i = 0; i < serviceNames.length; i++)
			{
				try
				{
					tsconnector.subscribeToServiceAsync(serviceNames[i]);
					System.out.println(serviceNames[i] + "\n");
				}catch(Exception ex )
				{
					System.out.println(serviceNames[i] + " - Subscription FAILED\n");
				}
			}
			if (serviceNames.length == 0) {
				System.out.println("There are no Tracking Services to subscribe to.\n");
			}
		}

	}
}

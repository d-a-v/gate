
/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

// http://code.google.com/p/wave-protocol/source/browse/src/com/google/gwt/websockets/client/
// adapted to GATE by David Gauchard

package fr.laas.gate;

import com.google.gwt.core.client.JavaScriptObject;

public class WebSocketClient
{
	
    ///////////////////////////////////////////////////////
	private final static class WebSocketImpl extends JavaScriptObject
	{
		public static native WebSocketImpl create (WebSocketClient client, String server, String protocol)
		/*-{
    		var ws = new WebSocket(server, protocol);

    		ws.onopen = $entry(function()
    		{
      			client.@fr.laas.gate.WebSocketClient::onOpen()();
    		});

    		ws.onclose = $entry(function()
    		{
      			client.@fr.laas.gate.WebSocketClient::onClose()();
      		});

      		ws.onerror= $entry(function()
      		{
      			client.@fr.laas.gate.WebSocketClient::onError()();
    		});

    		ws.onmessage = $entry(function(response)
    		{
    			client.@fr.laas.gate.WebSocketClient::onMessage(Ljava/lang/String;)(response.data);
    		});

    		return ws;
    	}-*/;

		public static native boolean isSupported()
		/*-{
    		return !!window.WebSocket;
    	}-*/;

		public native void close()
		/*-{
    		this.close();
    	}-*/;

		public native void send(String data)
		/*-{
    		this.send(data);
    	}-*/;

		protected WebSocketImpl()
		{
		}

	} // class WebSocketImpl
    ///////////////////////////////////////////////////////
	
	
	public WebSocketClient (WebSocketClientCallback callback, String server, String protocol)
	{
		if (!WebSocketImpl.isSupported())
			throw new RuntimeException("No WebSocket support");

		this.callback = callback;
		this.server = server;
		this.protocol = protocol;
		this.webSocket = null;
		this.guiObject = null;
		connected = false;
		closing = false;
	}
	
	public void setIntfObject (IntfObject o)
	{
		guiObject = o;
	}
	
	public IntfObject getIntfObject ()
	{
		return guiObject;
	}
	
	public String getServer ()
	{
		return server;
	}
	
	public boolean isLiving ()
	{
		return webSocket != null;
	}
	
	public boolean isConnected ()
	{
		return connected;
	}
	
	public void close ()
	{
		// prevent crash callback if currently trying to connect
		closing = true;
		
		Gate.debug("wsc::close");
		if (webSocket == null)
			throw new IllegalStateException("Not connected");
		webSocket.close();
		webSocket = null;
		connected = false;
		
		closing = false;
	}

	public void reconnect ()
	{
		if (webSocket != null)
			webSocket.close();
		webSocket = WebSocketImpl.create(this, server, protocol);
	}

	public void send (String data)
	{
		if (webSocket == null)
			throw new IllegalStateException("Not connected");
		webSocket.send(data);
	}

	@SuppressWarnings("unused")
	private void onClose ()
	{
		connected = false;
		Gate.debug("wsc::onClose");
		callback.onClose(this);
	}

	@SuppressWarnings("unused")
	private void onError ()
	{
		if (connected)
		{
			connected = false;
			if (!closing)
			{
				Gate.debug("wsc::onError");
				callback.onError(this);
			}
		}
	}

	@SuppressWarnings("unused")
	private void onMessage (String message)
	{
		callback.onMessage(this, message);
	}

	@SuppressWarnings("unused")
	private void onOpen ()
	{
		connected = true;
		callback.onConnect(this);
	}

	private final String server;
	private final String protocol;
	private final WebSocketClientCallback callback;
	private WebSocketImpl webSocket;

	private IntfObject guiObject;
    private boolean connected, closing;
}

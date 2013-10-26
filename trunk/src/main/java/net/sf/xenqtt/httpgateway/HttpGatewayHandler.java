/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.httpgateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class HttpGatewayHandler extends AbstractHandler {

	/**
	 * The handler sets the response status, content-type, and marks the request as handled before it generates the body of the response using a writer. There
	 * are a number of built in <a href="http://download.eclipse.org/jetty/stable-9/xref/org/eclipse/jetty/server/handler/package-summary.html">handlers</a>
	 * that might come in handy.
	 * 
	 * @param target
	 *            the target of the request, which is either a URI or a name from a named dispatcher.
	 * @param baseRequest
	 *            the Jetty mutable request object, which is always unwrapped.
	 * @param request
	 *            the immutable request object, which may have been wrapped by a filter or servlet.
	 * @param response
	 *            the response, which may have been wrapped by a filter or servlet.
	 * 
	 * @see org.eclipse.jetty.server.Handler#handle(java.lang.String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// FIXME [jim] - make sure http 1.1 persistent connections are being used on both server and client sides

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("http://www.google.com" + target);
		CloseableHttpResponse response1 = httpclient.execute(httpGet);
		try {
			response.setStatus(response1.getStatusLine().getStatusCode());
			HttpEntity entity = response1.getEntity();
			InputStream in = entity.getContent();
			OutputStream out = response.getOutputStream();
			for (int i = in.read(); i >= 0; i = in.read()) {
				out.write(i);
			}
		} finally {
			response1.close();
		}

		baseRequest.setHandled(true);

		// TODO Auto-generated method stub
	}
}

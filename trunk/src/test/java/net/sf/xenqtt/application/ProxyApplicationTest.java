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
package net.sf.xenqtt.application;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.application.ProxyApplication;
import net.sf.xenqtt.proxy.ProxyBroker;

import org.junit.Test;

public class ProxyApplicationTest {

	Map<String, String> args = new HashMap<String, String>();
	List<String> flags = new ArrayList<String>();
	AppContext context = new AppContext(flags, args, null);
	ProxyApplication app = new ProxyApplication();

	@Test(expected = IllegalStateException.class)
	public void testStart_NoBrokerArg() {

		app.start(context);
	}

	@Test
	public void testStart_NoPortArg() throws Exception {

		args.put("-b", "tcp://127.0.0.1:1234");
		context = new AppContext(flags, args, null);
		app.start(context);

		assertEquals(1883, getBroker().getPort());
		app.stop();
	}

	@Test
	public void testStart_WithPortArg() throws Exception {

		args.put("-b", "tcp://127.0.0.1:1234");
		args.put("-p", "19283");
		context = new AppContext(flags, args, null);
		app.start(context);

		assertEquals(19283, getBroker().getPort());
		app.stop();
	}

	@Test
	public void testStop_NotStarted() throws Exception {

		app.stop();
	}

	private ProxyBroker getBroker() throws Exception {
		Field field = ProxyApplication.class.getDeclaredField("broker");
		field.setAccessible(true);
		return (ProxyBroker) field.get(app);
	}
}

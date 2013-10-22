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
package net.sf.xenqtt.message;

import java.nio.channels.Selector;
import java.util.List;

/**
 * Wrapper that delegates directly to another {@link MqttChannel} implementation. This is used to allow changing the delegate to easily support things like
 * client reconnection.
 */
final class DelegatingMqttChannel implements MqttChannel {

	/**
	 * The channel to delegate to
	 */
	MqttChannel delegate;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param delegate
	 *            The {@link MqttChannel channel} that will serve as the delegate
	 */
	DelegatingMqttChannel(MqttChannel delegate) {
		this.delegate = delegate;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#deregister()
	 */
	@Override
	public void deregister() {

		delegate.deregister();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#register(java.nio.channels.Selector, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public boolean register(Selector selector, MessageHandler handler) {

		return delegate.register(selector, handler);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#finishConnect()
	 */
	@Override
	public boolean finishConnect() {

		return delegate.finishConnect();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#read(long)
	 */
	@Override
	public boolean read(long now) {

		return delegate.read(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#pauseRead()
	 */
	@Override
	public void pauseRead() {
		delegate.pauseRead();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#resumeRead()
	 */
	@Override
	public void resumeRead() {
		delegate.resumeRead();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#send(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public boolean send(MqttMessage message) {
		return delegate.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#send(net.sf.xenqtt.message.MqttMessage, net.sf.xenqtt.message.BlockingCommand)
	 */
	@Override
	public boolean send(MqttMessage message, BlockingCommand<MqttMessage> blockingCommand) {

		return delegate.send(message, blockingCommand);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#write(long)
	 */
	@Override
	public boolean write(long now) {

		return delegate.write(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close()
	 */
	@Override
	public void close() {

		delegate.close();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close(java.lang.Throwable)
	 */
	@Override
	public void close(Throwable cause) {

		delegate.close(cause);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isOpen()
	 */
	@Override
	public boolean isOpen() {

		return delegate.isOpen();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnected()
	 */
	@Override
	public boolean isConnected() {

		return delegate.isConnected();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnectionPending()
	 */
	@Override
	public boolean isConnectionPending() {

		return delegate.isConnectionPending();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#houseKeeping(long)
	 */
	@Override
	public long houseKeeping(long now) {

		return delegate.houseKeeping(now);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#sendQueueDepth()
	 */
	@Override
	public int sendQueueDepth() {

		return delegate.sendQueueDepth();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#inFlightMessageCount()
	 */
	@Override
	public int inFlightMessageCount() {

		return delegate.inFlightMessageCount();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#cancelBlockingCommands()
	 */
	@Override
	public void cancelBlockingCommands() {

		delegate.cancelBlockingCommands();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getUnsentMessages()
	 */
	@Override
	public List<MqttMessage> getUnsentMessages() {

		return delegate.getUnsentMessages();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getRemoteAddress()
	 */
	@Override
	public String getRemoteAddress() {

		return delegate.getRemoteAddress();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getLocalAddress()
	 */
	@Override
	public String getLocalAddress() {
		return delegate.getLocalAddress();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}
}

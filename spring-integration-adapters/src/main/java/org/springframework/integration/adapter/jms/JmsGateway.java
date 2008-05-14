/*
 * Copyright 2002-2008 the original author or authors.
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
 */

package org.springframework.integration.adapter.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * A message-driven adapter for receiving JMS messages and sending to a channel.
 * 
 * @author Mark Fisher
 */
public class JmsGateway extends SimpleMessagingGateway implements Lifecycle, DisposableBean {

	private volatile AbstractMessageListenerContainer container;

	private volatile ConnectionFactory connectionFactory;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile TaskExecutor taskExecutor;

	private volatile boolean sessionTransacted;

	private volatile int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private volatile int concurrentConsumers = 1;

	private volatile int maxConcurrentConsumers = 1;

	private volatile int maxMessagesPerTask = Integer.MIN_VALUE;

	private volatile int idleTaskExecutionLimit = 1;

	private boolean expectReply = false;


	public void setContainer(AbstractMessageListenerContainer container) {
		this.container = container;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	private void initialize() {
		if (this.container == null) {
			this.container = createDefaultContainer();
		}
		MessageListenerAdapter listener = new MessageListenerAdapter();
		listener.setDelegate(this);
		listener.setDefaultListenerMethod(this.expectReply ? "sendAndReceive" : "send");
		listener.setMessageConverter(this.messageConverter);
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
	}

	private AbstractMessageListenerContainer createDefaultContainer() {
		if (this.connectionFactory == null || (this.destination == null && this.destinationName == null)) {
			throw new ConfigurationException("If a 'container' reference is not provided, then "
					+ "'connectionFactory' and 'destination' (or 'destinationName') are required.");
		}
		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConcurrentConsumers(this.concurrentConsumers);
		dmlc.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
		dmlc.setMaxMessagesPerTask(this.maxMessagesPerTask);
		dmlc.setIdleTaskExecutionLimit(this.idleTaskExecutionLimit);
		dmlc.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			dmlc.setDestination(this.destination);
		}
		if (this.destinationName != null) {
			dmlc.setDestinationName(this.destinationName);
		}
		dmlc.setSessionTransacted(this.sessionTransacted);
		dmlc.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		dmlc.setAutoStartup(false);
		if (this.taskExecutor != null) {
			dmlc.setTaskExecutor(this.taskExecutor);
		}
		return dmlc;
	}

	public boolean isRunning() {
		return (this.container != null && this.container.isRunning());
	}

	public void start() {
		this.initialize();
		this.container.start();
	}

	public void stop() {
		if (this.container != null) {
			this.container.stop();
		}
	}

	public void destroy() {
		if (this.container != null) {
			this.container.destroy();
		}
	}

}

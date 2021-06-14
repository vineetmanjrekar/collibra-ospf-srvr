package uk.collibra.ospf;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.dsl.TcpServerConnectionFactorySpec;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
@Configuration
public class Application
{

	public static void main(String[] args) throws Exception
	{
		new SpringApplicationBuilder(Application.class)
				.run(args);
	}



	@Bean
	public MessageChannel outputChannel()
	{
		return new DirectChannel();
	}

	@Bean
	public MessageChannel tcpIn()
	{
		final DirectChannel directChannel = new DirectChannel();
		return directChannel;
	}

	@Bean
	public IntegrationFlow server(ServerSocketHandler serverSocketHandler) {
		return IntegrationFlows.from(Tcp.inboundGateway(
				Tcp.netServer(50000)
						.deserializer(TcpCodecs.lf())
						.serializer(TcpCodecs.lf())))
				.handle(serverSocketHandler::handleMessage)
				.get();
	}

	@Bean
	public ServerSocketHandler  serverSocketHandler() {
		return new ServerSocketHandler();
	}


	public class ServerSocketHandler
	{
		public String handleMessage(byte[] message, MessageHeaders messageHeaders)
		{
			String string = new String(message);
			return string.toUpperCase();
		}
	}

	@Bean
	public ApplicationListener<TcpConnectionOpenEvent> listener()
	{
		return new ApplicationListener<TcpConnectionOpenEvent>()
		{

			@Override
			public void onApplicationEvent(TcpConnectionOpenEvent event)
			{
				outputChannel().send(MessageBuilder.withPayload("HI, I AM " + UUID.randomUUID().toString())
						.setHeader(IpHeaders.CONNECTION_ID, event.getConnectionId())
						.build());
			}

		};
	}



}


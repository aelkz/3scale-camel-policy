package com.redhat.gtw.policy.route;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import com.redhat.gtw.policy.configuration.SSLProxyConfiguration;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("policy")
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

	@Autowired
	private SSLProxyConfiguration proxyConfig;

    @Override
    public void configure() throws Exception {

		final RouteDefinition from;

		// see: https://camel.apache.org/components/latest/netty-http-component.html
		from = from(proxyConfig.getConsumer()
			+":proxy://0.0.0.0:"
			+ proxyConfig.getPort()
			+ "?ssl=true&keyStoreFile="
			+ proxyConfig.getKeystoreDest()
			+ "&passphrase="
			+ proxyConfig.getKeystorePass()
			+ "&trustStoreFile="
			+ proxyConfig.getKeystoreDest());

		from
			.process((e) -> {
				System.out.println("\n:: camel policy request received\n");
			})
            .process(ProxyRoute::headers)
			.process((e) -> {
				System.out.println("\n:: forwarding request with "+ proxyConfig.getProducer() +" producer\n");
			})
			.toD(proxyConfig.getProducer()+":"
				+ "${headers." + Exchange.HTTP_SCHEME + "}://"
				+ "${headers." + Exchange.HTTP_HOST + "}:"
				+ "${headers." + Exchange.HTTP_PORT + "}"
				+ "${headers." + Exchange.HTTP_PATH + "}")
			.process((e) -> {
				System.out.println("\n:: request forwarded with "+ proxyConfig.getProducer() +" producer\n");
			})
		.end();
    }

    private static void headers(final Exchange exchange) {
    	LOGGER.info("BEFORE REDIRECT");
    	final Message message = exchange.getIn();
    	Iterator<String> iName = message.getHeaders().keySet().iterator();

    	LOGGER.info("header values:");
    	while(iName.hasNext()) {
    		String key = (String) iName.next();
    		LOGGER.info("\t[" +key+ "] - {"+message.getHeader(key)+"}");
    	}

		HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);

		LOGGER.info("");
		LOGGER.info("request header values:");
		Enumeration<String> headerNames = req.getHeaderNames();

		while(headerNames.hasMoreElements()){
			String element = headerNames.nextElement();
    		LOGGER.info("\t[" +element+ "] - {"+req.getHeader(element)+"}");
		}

		LOGGER.info("");
		LOGGER.info("REQUEST REMOTE Addr: " + req.getRemoteAddr());
		LOGGER.info("REQUEST REMOTE HOST: " + req.getRemoteHost());
		LOGGER.info("REQUEST REMOTE Request URI: " + req.getRequestURI());
		LOGGER.info("REQUEST REMOTE PORT: " + req.getRemotePort());
		LOGGER.info("REQUEST REMOTE USER: " + req.getRemoteUser());
		LOGGER.info("REQUEST PATH INFO: " + req.getPathInfo());
		LOGGER.info("REQUEST PATH Translated: " + req.getPathTranslated());
		LOGGER.info("REQUEST Server Name: " + req.getServerName());
		LOGGER.info("REQUEST Server Port: " + req.getServerPort());

		LOGGER.info("");
		LOGGER.info("REDIRECTING TO HTTP_HOST " + req.getServerName());
		LOGGER.info("REDIRECTING TO HTTP_PORT " + req.getServerPort());
		LOGGER.info("REDIRECTING TO HTTP_PATH " + req.getPathInfo());

		message.setHeader(Exchange.HTTP_HOST, req.getServerName());
		message.setHeader(Exchange.HTTP_PORT, req.getServerPort());
		message.setHeader(Exchange.HTTP_PATH, req.getPathInfo());

		LOGGER.info("");
		LOGGER.info("PROXY FORWARDING TO "
		+ message.getHeader(Exchange.HTTP_HOST)
		+":"+message.getHeader(Exchange.HTTP_PORT)
		+ message.getHeader(Exchange.HTTP_PATH));
    }

}

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
import com.redhat.gtw.policy.exception.RateLimitException;
// SSL configuration
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;

@Component("policy")
public class ProxyRoute extends RouteBuilder {
	private static final Logger LOGGER = Logger.getLogger(ProxyRoute.class.getName());

	@Autowired
	private SSLProxyConfiguration proxyConfig;

	private void configureSslForJetty() {
		KeyStoreParameters ksp = new KeyStoreParameters();
		ksp.setResource(proxyConfig.getKeystoreDest());
		ksp.setPassword(proxyConfig.getKeystorePass());

		KeyManagersParameters kmp = new KeyManagersParameters();
		kmp.setKeyStore(ksp);
		kmp.setKeyPassword(proxyConfig.getKeystorePass());

		SSLContextParameters scp = new SSLContextParameters();
		scp.setKeyManagers(kmp);

		JettyHttpComponent
				jettyComponent = getContext().getComponent("jetty", JettyHttpComponent.class);
		jettyComponent.setSslContextParameters(scp);
	}

	private void configureSslForHttp4() {
		KeyStoreParameters trust_ksp = new KeyStoreParameters();
		trust_ksp.setResource(proxyConfig.getKeystoreDest());
		trust_ksp.setPassword(proxyConfig.getKeystorePass());

		TrustManagersParameters trustp = new TrustManagersParameters();
		trustp.setKeyStore(trust_ksp);

		SSLContextParameters scp = new SSLContextParameters();
		scp.setTrustManagers(trustp);

		HttpComponent httpComponent = getContext().getComponent("https4", HttpComponent.class);
		httpComponent.setSslContextParameters(scp);
	}

    @Override
    public void configure() throws Exception {

		configureSslForJetty();
		//configureSslForHttp4();

		final RouteDefinition from;
		// from = from("jetty://http://0.0.0.0:8080?useXForwardedForHeader=true&matchOnUriPrefix=true");
		// from = from("jetty://https://0.0.0.0:8443?useXForwardedForHeader=true&matchOnUriPrefix=true");
		from = from("jetty://https://0.0.0.0:443?useXForwardedForHeader=true&matchOnUriPrefix=true");

		from
		.doTry()
			.process((e) -> {
				System.out.println(">>> beforeRedirect method");
			})
            .process(ProxyRoute::beforeRedirect)
				.process((e) -> {
					System.out.println(">>> forwarding request to backend");
				})
			.toD("http4://"
				+ "${header." + Exchange.HTTP_HOST + "}:"
				+ "${header." + Exchange.HTTP_PORT + "}"
				+ "${header." + Exchange.HTTP_PATH + "}"
				+ "?connectionClose=false&bridgeEndpoint=true&copyHeaders=true"
			)
			.process((e) -> {
				System.out.println(">>> afterRedirect method");
			})
            .process(ProxyRoute::afterRedirect)
		  .endDoTry()
          .doCatch(RateLimitException.class)
			.process((e) -> {
				System.out.println(">>> afterRedirect method");
			})
		    .process(ProxyRoute::sendRateLimitErro)
		  .end();
    }

    private static void beforeRedirect(final Exchange exchange) throws RateLimitException {
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

		if(true) { // validation constraints (TODO)
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
       }else {
    	   LOGGER.info("RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr());
    	   throw new RateLimitException("RATE LIMIT REACHED FOR IP "+ req.getRemoteAddr() );
       }
    }
    
    private static void afterRedirect(final Exchange exchange) {
    	LOGGER.info("AFTER REDIRECT ");
    }
    
    private static void sendRateLimitErro(final Exchange exchange) {
    	LOGGER.info("SEND COD ERROR 429");
    	final Message message = exchange.getIn();
    	message.setHeader(Exchange.HTTP_RESPONSE_CODE,429);
    }

}

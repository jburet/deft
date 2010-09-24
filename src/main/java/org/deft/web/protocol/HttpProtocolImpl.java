package org.deft.web.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.deft.util.HttpUtil;
import org.deft.web.Application;
import org.deft.web.HttpVerb;
import org.deft.web.handler.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProtocolImpl implements HttpProtocol {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocolImpl.class);
	
	private static final int BUFFER_SIZE = 512;	//in bytes

	private final Application application;
	
	public HttpProtocolImpl(Application app) {
		application = app;
	}
	
	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		//logger.debug("Received accept event");
		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel clientChannel = (SocketChannel) key.channel();
		HttpRequest request = getHttpRequest(key, clientChannel);
		HttpResponse response = new HttpResponse(clientChannel);
		
		RequestHandler rh = application.getHandler(request.getRequestedPath());
		if (rh != null) {
			HttpRequestDispatcher.dispatch(rh, request, response);
		} else {
			String _404 = HttpUtil.createHttpHeader(404);
			response.write(_404);
			response.write("Requested URL: " + request.getRequestedPath() + " was not found");
		}
		
		//Only close if not async. In that case its up to RH to close it
		if (!rh.isMethodAsynchronous(request.getMethod())) {
			clientChannel.close();	// remove this line ()
		}
	}

	@Override
	public void handleWrite(SelectionKey key) throws IOException {
		logger.debug("Received write event");
		// TODO Auto-generated method stub

	}

	
	
	private HttpRequest getHttpRequest(SelectionKey key, SocketChannel clientChannel) {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		try {
			long bytesRead = clientChannel.read(buffer);
		} catch (IOException e) {
			logger.error("Could not read buffer: {}", e);
		}
		return HttpRequest.of(buffer);
	}
	


}

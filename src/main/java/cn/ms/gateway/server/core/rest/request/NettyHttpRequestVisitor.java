package cn.ms.gateway.server.core.rest.request;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ms.gateway.server.common.HttpConstants;
import cn.ms.gateway.server.common.RequestMethod;
import cn.ms.gateway.server.common.utils.HttpUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;

public class NettyHttpRequestVisitor implements HttpRequestVisitor {

	private final Channel channel;
	private final FullHttpRequest request;

	public NettyHttpRequestVisitor(Channel channel, FullHttpRequest request) {
		this.channel = channel;
		this.request = request;
	}

	@Override
	public String visitRemoteAddress() {
		for (Map.Entry<String, String> entry : request.headers()) {
			if (entry.getKey().equals(HttpConstants.HEADER_X_FORWARDED_FOR)) {
				return entry.getValue();
			}
		}
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}

	@Override
	public RequestMethod visitHttpMethod() {
		return HttpUtils.convertHttpMethodFromNetty(request);
	}

	@Override
	public String visitHttpBody() {
		return request.content().toString(CharsetUtil.UTF_8);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Map<String, String> visitHttpParams() {
		Map<String, String> params = new HashMap<>(32);

		// from URL
		QueryStringDecoder decoder = new QueryStringDecoder(request.getUri(), Charset.forName("UTF-8"));
		for (Map.Entry<String, List<String>> item : decoder.parameters().entrySet()) {
			params.put(item.getKey(), item.getValue().get(0));
		}

		// query string and body
		if (visitHttpMethod() != RequestMethod.GET) {
			// from content body key-value
			QueryStringDecoder kvDecoder = new QueryStringDecoder(visitHttpBody(), Charset.forName("UTF-8"), false);
			for (Map.Entry<String, List<String>> item : kvDecoder.parameters().entrySet()) {
				params.put(item.getKey(), item.getValue().get(0));
			}
		}

		return params;
	}

	@Override
	public Map<String, String> visitHttpHeaders() {
		Map<String, String> headers = new HashMap<>(32);
		for (Map.Entry<String, String> entry : request.headers()){
			headers.put(entry.getKey(), entry.getValue());
		}
		
		return headers;
	}

	@SuppressWarnings("deprecation")
	@Override
	public String visitURI() {
		return request.getUri();
	}

	@SuppressWarnings("deprecation")
	@Override
	public String[] visitTerms() {
		String termsUrl = HttpUtils.truncateUrl(request.getUri());
		return FluentIterable.from(Splitter.on('/').omitEmptyStrings().trimResults().split(termsUrl)).toArray(String.class);
	}

	@SuppressWarnings("deprecation")
	@Override
	public HttpVersion visitHttpVersion() {
		return request.getProtocolVersion();
	}
}

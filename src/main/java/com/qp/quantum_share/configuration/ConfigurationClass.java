package com.qp.quantum_share.configuration;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.dto.PaymentDetails;
import com.qp.quantum_share.dto.SocialMediaPosts;
import com.qp.quantum_share.response.ErrorResponse;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import com.qp.quantum_share.response.SuccessResponse;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;

@Component
public class ConfigurationClass {

	@Bean
	public WebMvcConfigurer configurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry reg) {
				reg.addMapping("/**").allowedOrigins("*").allowedMethods("*");
			}
		};
	}

	@Bean
	public HttpHeaders httpHeaders() {
		return new HttpHeaders();
	}

	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(String jsonString, HttpHeaders headers) {
		return new HttpEntity<>(jsonString, headers);
	}

	@Bean
	@Lazy
	public HttpEntity<MultiValueMap<String, Object>> getHttpEntityWithMap(MultiValueMap<String, Object> multiValueMap,
			HttpHeaders headers) {
		return new HttpEntity<>(multiValueMap, headers);
	}

	@Bean
	public ObjectMapper getMapper() {
		 ObjectMapper objectMapper = new ObjectMapper();
	        objectMapper.registerModule(new JavaTimeModule());
	        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	        return objectMapper;
	}
	
	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(HttpHeaders headers) {
		return new HttpEntity<>(headers);
	}

	@Bean
	@Lazy
	public Map<String, Object> getMap() {
		return new HashMap<String, Object>();
	}

	@Bean
	public MultiValueMap<String, Object> getMultiValueMap() {
		return new LinkedMultiValueMap<String, Object>();
	}

	@Bean
	public RestTemplate getRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(30000);

		return new RestTemplate(factory);
	}

	@Bean
	public FacebookPageDetails pageDetails() {
		return new FacebookPageDetails();
	}

	@Bean
	@Lazy
	public FacebookClient getFacebookClient(String accessToken) {
		return new DefaultFacebookClient(accessToken, Version.LATEST);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ResponseStructure<String> structure) {
		return new ResponseWrapper(structure);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(SuccessResponse successResponse) {
		return new ResponseWrapper(successResponse);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ErrorResponse errorResponse) {
		return new ResponseWrapper(errorResponse);
	}

	@Bean
	public List<Object> getList() {
		return new ArrayList<Object>();
	}

	@Bean
	@Lazy
	public List<SocialMediaPosts> getListOfPost() {
		return new ArrayList<SocialMediaPosts>();
	}

	@Bean
	@Lazy
	public List<PaymentDetails> getPaymentList() {
		return new ArrayList<PaymentDetails>();
	}

	@Bean
	public SuccessResponse getSuccessResponse() {
		return new SuccessResponse();
	}

	@Bean
	public ErrorResponse getErrorResponse() {
		return new ErrorResponse();
	}

	@Bean
	@Lazy
	public PaymentDetails paymentDetails() {
		return new PaymentDetails();
	}

	@Bean
	public SecureRandom secureRandom() {
		return new SecureRandom();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StringBuilder stringBuilder() {
		return new StringBuilder();
	}

	@Bean
	public ByteArrayResourceFactory byteArrayResourceFactory() {
		return new ByteArrayResourceFactory();
	}

	public static class ByteArrayResourceFactory {
		public ByteArrayResource createByteArrayResource(byte[] byteArray, String filename) {
			return new ByteArrayResource(byteArray) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
		}
	}

	@Bean
	@Lazy
	public HttpEntity<Map<String, Object>> getMapHttpEntity(Map<String, Object> body, HttpHeaders headers) {
		return new HttpEntity<>(body, headers);
	}

	@Bean
	@Lazy
	public SocialMediaPosts getsocialMediaPosts() {
		return new SocialMediaPosts();
	}

	@Bean
	@Lazy
	public HttpEntity<byte[]> getByteHttpEntity(byte[] byteArray, HttpHeaders headers) {
		return new HttpEntity<>(byteArray, headers);
	}
}

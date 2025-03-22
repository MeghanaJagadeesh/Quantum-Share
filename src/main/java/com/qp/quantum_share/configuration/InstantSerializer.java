package com.qp.quantum_share.configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

@Configuration
public class InstantSerializer extends JsonSerializer<Instant> {

	 private static final DateTimeFormatter FORMATTER = DateTimeFormatter
	            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
	            .withZone(ZoneOffset.UTC);
	 
	@Override
	public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		 gen.writeString(FORMATTER.format(value));
	} 
	
	
//	  @Bean
//	    public ObjectMapper objectMapper() {
//	        ObjectMapper mapper = new ObjectMapper();
//	        mapper.registerModule(new JavaTimeModule());
//	        return mapper;
//	    }
//
//	    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//	        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//	        converter.setObjectMapper(objectMapper());
//	        converters.add(converter);
//	    }
	}

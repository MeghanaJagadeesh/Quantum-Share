package com.qp.quantum_share.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
//@EnableWebMvc
public class SpringFoxConfig implements WebMvcConfigurer { // Implement WebMvcConfigurer

//	@Bean
//	public Docket api() {
//		return new Docket(DocumentationType.SWAGGER_2).select()
//				.apis(RequestHandlerSelectors.basePackage("com.qp.quantum_share")).paths(PathSelectors.regex("/.*"))
//				.build().apiInfo(apiInfo());
//	}
//
//	private ApiInfo apiInfo() {
//		return new ApiInfoBuilder().title("Quantumshare API")
//				.description("Quantumshare a Social Media Management System https://quantumshare.quantumparadigm.in/")
//				.version("1.0.0").build();
//	}

//	@Override
//	public void addResourceHandlers(ResourceHandlerRegistry registry) {
//		registry.addResourceHandler("swagger-ui/**")
//				.addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
//		registry.addResourceHandler("v2/api-docs").addResourceLocations("classpath:/META-INF/resources/");
//	}

//	 @Bean
//	    public OpenApiResource openApiResource() {
//	        return new OpenApiResource();
//	    }

//	@Bean
//	public OpenAPI customOpenAPI() {
//		return new OpenAPI().info(new Info().title("Quantum Share API").version("1.0")
//				.description("API documentation for Quantum Share"));
//	}
}

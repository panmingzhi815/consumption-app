package com.dongluhitec.consumptionapp;

import com.dongluhitec.consumptionapp.service.OnecardService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public FilterRegistrationBean testFilterRegistration() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new OnecardService());//添加过滤器
		registration.addUrlPatterns("/*");//设置过滤路径，/*所有路径
		registration.setName("OnecardService");//设置优先级
		registration.setOrder(1);//设置优先级
		return registration;
	}
}

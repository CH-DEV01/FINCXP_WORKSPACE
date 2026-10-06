package com.davivienda.financiamiento;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionTrackingMode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.EnumSet;

@SpringBootApplication
public class FinanciamientoApplication extends SpringBootServletInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        // OWASP A02 — forzar cookie-only: el JSESSIONID nunca aparece en la URL,
        // sin importar si corre en Tomcat embebido o externo.
        servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(FinanciamientoApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanciamientoApplication.class, args);
    }
}

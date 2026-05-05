package com.calendario.callapp.callapp_backend.service.impl;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PlantillaCorreoServiceImpl {

    public String render(String nombrePlantilla, Map<String, String> variables) {
        try {
            String contenido = new String(
                    new ClassPathResource("mail-templates/" + nombrePlantilla).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                contenido = contenido.replace("${" + variable.getKey() + "}", variable.getValue() != null ? variable.getValue() : "");
            }
            return contenido;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible cargar la plantilla de correo");
        }
    }
}

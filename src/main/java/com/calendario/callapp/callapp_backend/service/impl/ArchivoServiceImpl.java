package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.response.ArchivoResponse;
import com.calendario.callapp.callapp_backend.entity.ArchivoAdjunto;
import com.calendario.callapp.callapp_backend.repository.ArchivoAdjuntoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchivoServiceImpl {

    private final ArchivoAdjuntoRepository archivoAdjuntoRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir = "uploads";

    @Value("${file.allowed-extensions:jpg,jpeg,png,pdf}")
    private String allowedExtensions = "jpg,jpeg,png,pdf";

    @Value("${file.allowed-content-types:image/jpeg,image/png,application/pdf}")
    private String allowedContentTypes = "image/jpeg,image/png,application/pdf";

    @Value("${file.max-size-bytes:10485760}")
    private long maxSizeBytes;

    private Path rootLocation = Paths.get(".").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible inicializar la carpeta de archivos");
        }
    }

    public ArchivoResponse guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes adjuntar un archivo");
        }
        validarArchivo(archivo);

        String nombreOriginal = obtenerNombreOriginalSeguro(archivo);
        String extension = "";
        int extensionIndex = nombreOriginal.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = nombreOriginal.substring(extensionIndex);
        }

        String nombreArchivo = UUID.randomUUID() + extension;
        Path destino = rootLocation.resolve(nombreArchivo);

        try {
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar el archivo");
        }

        ArchivoAdjunto metadata = new ArchivoAdjunto();
        metadata.setNombreOriginal(nombreOriginal);
        metadata.setNombreAlmacenado(nombreArchivo);
        metadata.setTokenAcceso(UUID.randomUUID().toString().replace("-", ""));
        metadata.setContentType(archivo.getContentType());
        metadata.setTamano(archivo.getSize());
        metadata.setPublico(true);
        metadata.setFechaCreacion(java.time.LocalDateTime.now());
        metadata = archivoAdjuntoRepository.save(metadata);

        return ArchivoResponse.builder()
                .id(metadata.getId())
                .nombreArchivo(nombreArchivo)
                .nombreOriginal(nombreOriginal)
                .tokenAcceso(metadata.getTokenAcceso())
                .url("/archivos/public/" + metadata.getTokenAcceso())
                .contentType(metadata.getContentType())
                .tamano(archivo.getSize())
                .build();
    }

    public Resource cargarPublicoComoRecurso(String tokenAcceso) {
        try {
            if (tokenAcceso == null || !tokenAcceso.matches("^[a-zA-Z0-9]{20,}$")) {
                return cargarRecursoFallback();
            }
            
            ArchivoAdjunto metadata = archivoAdjuntoRepository.findByTokenAccesoAndPublicoTrue(tokenAcceso)
                    .orElse(null);
            
            if (metadata == null) {
                return cargarRecursoFallback();
            }

            Path archivo = rootLocation.resolve(metadata.getNombreAlmacenado()).normalize();
            if (!archivo.startsWith(rootLocation)) {
                return cargarRecursoFallback();
            }
            
            String archivoUri = Objects.requireNonNull(archivo.toUri()).toString();
            Resource recurso = new UrlResource(Objects.requireNonNull(archivoUri));
            
            if (!recurso.exists() || !recurso.isReadable()) {
                return cargarRecursoFallback();
            }
            
            return recurso;
        } catch (Exception ex) {
            return cargarRecursoFallback();
        }
    }

    private Resource cargarRecursoFallback() {
        try {
            return new org.springframework.core.io.ClassPathResource("static/assets/img/default-avatar.png");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no encontrado y fallback fallido");
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo supera el tamano maximo permitido");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null) {
            int extensionIndex = nombreOriginal.lastIndexOf('.');
            if (extensionIndex >= 0) {
                extension = nombreOriginal.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
            }
        }

        Set<String> extensionesPermitidas = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
        if (!extensionesPermitidas.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extension de archivo no permitida");
        }

        Set<String> contentTypesPermitidos = Arrays.stream(allowedContentTypes.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
        String contentType = archivo.getContentType();
        if (contentType == null || !contentTypesPermitidos.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no permitido");
        }
    }

    private String obtenerNombreOriginalSeguro(MultipartFile archivo) {
        String nombreCrudo = archivo.getOriginalFilename();
        String nombreOriginal = StringUtils.cleanPath(nombreCrudo == null ? "archivo" : nombreCrudo);
        if (nombreOriginal.isBlank()) {
            return "archivo";
        }
        return nombreOriginal;
    }

    void configurarParaPruebas(String uploadDir, String allowedExtensions, String allowedContentTypes, long maxSizeBytes) {
        this.uploadDir = uploadDir;
        this.allowedExtensions = allowedExtensions;
        this.allowedContentTypes = allowedContentTypes;
        this.maxSizeBytes = maxSizeBytes;
        init();
    }
}

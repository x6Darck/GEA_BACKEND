package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.response.ArchivoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para operaciones de almacenamiento de archivos.
 * La implementación por defecto usa el filesystem local (ArchivoServiceImpl).
 * En producción se puede sustituir por S3, Cloudinary, etc. sin tocar el resto del código.
 */
public interface ArchivoStorageService {

    /**
     * Guarda el archivo y retorna la entidad ArchivoAdjunto persistida como DTO de respuesta.
     */
    ArchivoResponse guardar(MultipartFile archivo);

    /**
     * Carga un archivo por su token de acceso público y lo devuelve como Resource descargable.
     */
    Resource cargarPublicoComoRecurso(String tokenAcceso);
}

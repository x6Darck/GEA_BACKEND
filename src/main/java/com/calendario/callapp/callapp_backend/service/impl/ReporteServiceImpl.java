package com.calendario.callapp.callapp_backend.service.impl;

import com.calendario.callapp.callapp_backend.dto.request.ActualizarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.request.GenerarReporteRequest;
import com.calendario.callapp.callapp_backend.dto.common.GrupoDatoDTO;
import com.calendario.callapp.callapp_backend.dto.response.ReporteDashboardDTO;
import com.calendario.callapp.callapp_backend.dto.response.ReporteGeneradoResponse;
import com.calendario.callapp.callapp_backend.dto.response.ReporteSolicitudesResponse;
import com.calendario.callapp.callapp_backend.dto.response.SolicitudResumenDTO;
import com.calendario.callapp.callapp_backend.entity.EstadoSolicitud;
import com.calendario.callapp.callapp_backend.entity.Oficina;
import com.calendario.callapp.callapp_backend.entity.ReporteGenerado;
import com.calendario.callapp.callapp_backend.entity.SolicitudAnuncio;
import com.calendario.callapp.callapp_backend.entity.SolicitudEvento;
import com.calendario.callapp.callapp_backend.entity.Usuario;
import com.calendario.callapp.callapp_backend.repository.ReporteGeneradoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.calendario.callapp.callapp_backend.repository.SolicitudAnuncioRepository;
import com.calendario.callapp.callapp_backend.repository.SolicitudEventoRepository;
import com.calendario.callapp.callapp_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl {

    private final SolicitudEventoRepository solicitudEventoRepository;
    private final SolicitudAnuncioRepository solicitudAnuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReporteGeneradoRepository reporteGeneradoRepository;
    
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public ReporteGeneradoResponse crearReporte(GenerarReporteRequest request, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        validarFechas(request.getDesde(), request.getHasta());

        String formato = normalizarFormato(request.getFormato());

        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setNombre(request.getNombre());
        reporte.setDescripcion(request.getDescripcion());
        reporte.setFormato(formato);
        reporte.setFechaDesde(request.getDesde());
        reporte.setFechaHasta(request.getHasta());
        reporte.setAlcance(request.getAlcance());
        reporte.setFechaCreacion(LocalDateTime.now());
        reporte.setUsuarioGenerador(usuario);
        reporte.setIdOficina(request.getIdOficina());
        reporte.setIdTipoEvento(request.getIdTipoEvento());

        return toReporteGeneradoResponse(reporteGeneradoRepository.save(reporte));
    }

    @Transactional(readOnly = true)
    public List<ReporteGeneradoResponse> listarReportes(
            Long filterOficinaId,
            LocalDate filterFechaDesde,
            LocalDate filterFechaHasta,
            Authentication authentication) {

        Usuario usuario = obtenerUsuario(authentication);
        List<ReporteGenerado> reportes;

        if (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) {
            reportes = reporteGeneradoRepository.findAll();
        } else if (usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) {
            if (usuario.getOficina() == null) {
                reportes = reporteGeneradoRepository.findByUsuarioGeneradorIdOrderByFechaCreacionDesc(usuario.getId());
            } else {
                reportes = reporteGeneradoRepository
                        .findByUsuarioGeneradorOficinaIdOrderByFechaCreacionDesc(usuario.getOficina().getId());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para listar reportes");
        }

        return reportes.stream()
                .filter(r -> {
                    if (filterOficinaId != null) {
                        if (r.getUsuarioGenerador().getOficina() == null) return false;
                        if (!r.getUsuarioGenerador().getOficina().getId().equals(filterOficinaId)) return false;
                    }
                    if (filterFechaDesde != null) {
                        if (r.getFechaCreacion().toLocalDate().isBefore(filterFechaDesde)) return false;
                    }
                    if (filterFechaHasta != null) {
                        if (r.getFechaCreacion().toLocalDate().isAfter(filterFechaHasta)) return false;
                    }
                    return true;
                })
                .sorted((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()))
                .map(this::toReporteGeneradoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteGeneradoResponse> listarReportes(Authentication authentication) {
        return listarReportes(null, null, null, authentication);
    }

    @Transactional(readOnly = true)
    public byte[] exportarReporteGenerado(Long reporteId, Authentication authentication) {
        ReporteGenerado reporte = buscarReporte(reporteId, authentication);
        DatosReporte datos = obtenerDatosReporte(
                reporte.getFechaDesde(),
                reporte.getFechaHasta(),
                reporte.getAlcance(),
                reporte.getIdOficina(),
                reporte.getIdTipoEvento(),
                authentication);

        if ("PDF".equals(reporte.getFormato())) {
            return exportarPdfInterno(reporte.getFechaDesde(), reporte.getFechaHasta(), datos, authentication);
        } else {
            return exportarCsvInterno(datos);
        }
    }

    @Transactional(readOnly = true)
    public ReporteDashboardDTO obtenerDashboardStats(Long oficinaId, LocalDate desde, LocalDate hasta, Authentication authentication) {
        try {
            validarFechas(desde, hasta);
            LocalDateTime fechaDesde = desde.atStartOfDay();
            LocalDateTime fechaHasta = hasta.atTime(LocalTime.MAX);

            List<Object[]> porTipo = solicitudEventoRepository.countEventosByTipoFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> evOficina = solicitudEventoRepository.countEventosByOficinaFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> evEstado = solicitudEventoRepository.countEventosByEstadoFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> evMes = solicitudEventoRepository.countEventosByMesFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> anOficina = solicitudAnuncioRepository.countAnunciosByOficinaFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> anEstado = solicitudAnuncioRepository.countAnunciosByEstadoFiltered(oficinaId, fechaDesde, fechaHasta);
            List<Object[]> anMes = solicitudAnuncioRepository.countAnunciosByMesFiltered(oficinaId, fechaDesde, fechaHasta);

            long totalEventos = solicitudEventoRepository.countFiltered(oficinaId, fechaDesde, fechaHasta);
            long totalAnuncios = solicitudAnuncioRepository.countFiltered(oficinaId, fechaDesde, fechaHasta);
            long total = totalEventos + totalAnuncios;

            // Optimización: Extraer conteos de los mapas de estado ya obtenidos para evitar ráfaga de consultas extra
            java.util.Map<EstadoSolicitud, Long> evEstadoMap = new java.util.HashMap<>();
            evEstado.forEach(r -> evEstadoMap.put((EstadoSolicitud)r[0], (Long)r[1]));
            
            java.util.Map<EstadoSolicitud, Long> anEstadoMap = new java.util.HashMap<>();
            anEstado.forEach(r -> anEstadoMap.put((EstadoSolicitud)r[0], (Long)r[1]));

            long aprobados = evEstadoMap.getOrDefault(EstadoSolicitud.APROBADA, 0L) + evEstadoMap.getOrDefault(EstadoSolicitud.PUBLICADA, 0L)
                           + anEstadoMap.getOrDefault(EstadoSolicitud.APROBADA, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.PUBLICADA, 0L);
            
            long pendientes = evEstadoMap.getOrDefault(EstadoSolicitud.PENDIENTE, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.PENDIENTE, 0L);
                
            long rechazados = evEstadoMap.getOrDefault(EstadoSolicitud.RECHAZADA, 0L) + anEstadoMap.getOrDefault(EstadoSolicitud.RECHAZADA, 0L);

            double tasa = total > 0 ? (double) aprobados / total * 100 : 0;

            List<SolicitudEvento> evList = (oficinaId != null) 
                ? solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(oficinaId, fechaDesde, fechaHasta)
                : solicitudEventoRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(fechaDesde, fechaHasta);
            
            List<SolicitudAnuncio> anList = (oficinaId != null)
                ? solicitudAnuncioRepository.findByOficinaIdAndFechaCreacionBetweenOrderByFechaCreacionDesc(oficinaId, fechaDesde, fechaHasta)
                : solicitudAnuncioRepository.findByFechaCreacionBetweenOrderByFechaCreacionDesc(fechaDesde, fechaHasta);

            List<SolicitudResumenDTO> solicitudesDetalle = new java.util.ArrayList<>();
            evList.stream().limit(10).forEach(e -> solicitudesDetalle.add(mapToResumen(e)));
            anList.stream().limit(10).forEach(a -> solicitudesDetalle.add(mapToResumen(a)));
            solicitudesDetalle.sort((a, b) -> b.getFechaRegistro().compareTo(a.getFechaRegistro()));

            ReporteDashboardDTO dto = ReporteDashboardDTO.builder()
                    .totalSolicitudes(total)
                    .totalAprobados(aprobados)
                    .totalPendientes(pendientes)
                    .totalRechazados(rechazados)
                    .tasaAprobacion(Math.round(tasa * 100.0) / 100.0)
                    .eventosPorTipo(mapToGrupoDato(porTipo).stream()
                            .filter(g -> !g.getEtiqueta().trim().equals("-") && !g.getEtiqueta().trim().isEmpty() && !g.getEtiqueta().equals("Sin Tipo"))
                            .toList())
                    .solicitudesPorOficina(mapToGrupoDato(combinarResultados(evOficina, anOficina)))
                    .tendenciaEstado(mapToGrupoDato(combinarResultados(evEstado, anEstado)))
                    .solicitudesPorMes(fillMonthGaps(combinarResultados(evMes, anMes)))
                    .solicitudes(solicitudesDetalle)
                    .build();
            
            entityManager.flush();
            return dto;
        } catch (Exception e) {
            log.error("Error generando estadísticas de dashboard", e);
            return ReporteDashboardDTO.builder()
                    .totalSolicitudes(0L).totalAprobados(0L).totalPendientes(0L).totalRechazados(0L)
                    .tasaAprobacion(0.0)
                    .eventosPorTipo(new java.util.ArrayList<>())
                    .solicitudesPorOficina(new java.util.ArrayList<>())
                    .tendenciaEstado(new java.util.ArrayList<>())
                    .solicitudesPorMes(new java.util.ArrayList<>())
                    .build();
        }
    }

    private List<Object[]> combinarResultados(List<Object[]> lista1, List<Object[]> lista2) {
        java.util.Map<Object, Object[]> mapa = new java.util.HashMap<>();
        if (lista1 != null) {
            for (Object[] r : lista1) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    long valor = r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L;
                    String color = r.length >= 3 && r[2] != null ? r[2].toString() : null;
                    mapa.put(r[0], new Object[]{valor, color});
                }
            }
        }
        if (lista2 != null) {
            for (Object[] r : lista2) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    Object key = r[0];
                    long valor = r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L;
                    String color = r.length >= 3 && r[2] != null ? r[2].toString() : null;
                    if (mapa.containsKey(key)) {
                        Object[] exist = mapa.get(key);
                        exist[0] = ((Long)exist[0]) + valor;
                        if (exist[1] == null && color != null) exist[1] = color;
                    } else {
                        mapa.put(key, new Object[]{valor, color});
                    }
                }
            }
        }
        List<Object[]> resultado = new java.util.ArrayList<>();
        mapa.forEach((k, v) -> resultado.add(new Object[]{k, v[0], v[1]}));
        return resultado;
    }

    private List<GrupoDatoDTO> mapToGrupoDato(List<Object[]> raw) {
        if (raw == null) return new java.util.ArrayList<>();
        return raw.stream()
                .filter(r -> r != null && r.length >= 2 && r[0] != null)
                .map(r -> {
                    GrupoDatoDTO dto = new GrupoDatoDTO(
                        r[0].toString(), 
                        r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L
                    );
                    if (r.length >= 3 && r[2] != null) dto.setColor(r[2].toString());
                    return dto;
                })
                .sorted((a, b) -> b.getValor().compareTo(a.getValor()))
                .toList();
    }

    private List<GrupoDatoDTO> fillMonthGaps(List<Object[]> raw) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        java.util.Map<Integer, Long> dataMap = new java.util.HashMap<>();
        for (int i = 1; i <= 12; i++) dataMap.put(i, 0L);
        if (raw != null) {
            for (Object[] r : raw) {
                int m;
                if (r[0] instanceof Number) m = ((Number) r[0]).intValue();
                else {
                    try { m = Integer.parseInt(r[0].toString()); } catch (Exception e) { continue; }
                }
                dataMap.put(m, r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L);
            }
        }
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new GrupoDatoDTO(meses[i - 1], dataMap.get(i)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteSolicitudesResponse generarResumen(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        List<SolicitudEvento> eventos = datos.eventos();
        List<SolicitudAnuncio> anuncios = datos.anuncios();

        return ReporteSolicitudesResponse.builder()
                .alcance(datos.alcance())
                .desde(desde)
                .hasta(hasta)
                .totalSolicitudesEvento(eventos.size())
                .totalSolicitudesAnuncio(anuncios.size())
                .eventosPendientes(contarEventos(eventos, EstadoSolicitud.PENDIENTE))
                .eventosAprobados(contarEventos(eventos, EstadoSolicitud.APROBADA))
                .eventosRechazados(contarEventos(eventos, EstadoSolicitud.RECHAZADA))
                .eventosPublicados(contarEventos(eventos, EstadoSolicitud.PUBLICADA))
                .anunciosPendientes(contarAnuncios(anuncios, EstadoSolicitud.PENDIENTE))
                .anunciosAprobados(contarAnuncios(anuncios, EstadoSolicitud.APROBADA))
                .anunciosRechazados(contarAnuncios(anuncios, EstadoSolicitud.RECHAZADA))
                .anunciosPublicados(contarAnuncios(anuncios, EstadoSolicitud.PUBLICADA))
                .solicitudes(mapearSolicitudes(eventos, anuncios))
                .build();
    }

    private List<SolicitudResumenDTO> mapearSolicitudes(List<SolicitudEvento> eventos, List<SolicitudAnuncio> anuncios) {
        List<SolicitudResumenDTO> lista = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        eventos.forEach(e -> lista.add(SolicitudResumenDTO.builder()
                .id(e.getId())
                .tipo("EVENTO")
                .titulo(e.getNombreEvento())
                .oficina(e.getOficina() != null ? e.getOficina().getNombre() : "N/A")
                .fechaRegistro(e.getFechaCreacion() != null ? e.getFechaCreacion().format(dtf) : "-")
                .estado(String.valueOf(e.getEstado()))
                .build()));
        anuncios.forEach(a -> lista.add(SolicitudResumenDTO.builder()
                .id(a.getId())
                .tipo("ANUNCIO")
                .titulo(a.getTitulo())
                .oficina(a.getOficina() != null ? a.getOficina().getNombre() : (a.getCategoria() != null ? a.getCategoria() : "N/A"))
                .fechaRegistro(a.getFechaCreacion() != null ? a.getFechaCreacion().format(dtf) : "-")
                .estado(String.valueOf(a.getEstado()))
                .build()));
        lista.sort((a, b) -> b.getFechaRegistro().compareTo(a.getFechaRegistro()));
        return lista;
    }

    @Transactional(readOnly = true)
    public byte[] exportarCsv(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        return exportarCsvInterno(datos);
    }

    @Transactional(readOnly = true)
    public byte[] exportarPdf(LocalDate desde, LocalDate hasta, Authentication authentication) {
        DatosReporte datos = obtenerDatosReporte(desde, hasta, "GLOBAL", null, null, authentication);
        return exportarPdfInterno(desde, hasta, datos, authentication);
    }

    private byte[] exportarPdfInterno(LocalDate desde, LocalDate hasta, DatosReporte datos,
            Authentication authentication) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22,
                    com.lowagie.text.Font.NORMAL, java.awt.Color.decode("#CE1126"));
            Paragraph header = new Paragraph("CALLAPP - SISTEMA DE GESTIÓN", titleFont);
            header.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(header);
            Paragraph subHeader = new Paragraph("Reporte de Auditoría: " + datos.alcance(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            subHeader.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(subHeader);
            document.add(new Paragraph(" "));
            com.lowagie.text.Font metadataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            document.add(new Paragraph("Periodo Consultado: " + desde + " al " + hasta, metadataFont));
            document.add(new Paragraph("Generado el: " + LocalDateTime.now().withNano(0), metadataFont));
            document.add(new Paragraph("Responsable: " + authentication.getName(), metadataFont));
            document.add(new Paragraph(" "));
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10f);
            summaryTable.setSpacingAfter(10f);
            agregarHeader(summaryTable, "SOLICITUDES");
            agregarHeader(summaryTable, "APROBADAS");
            agregarHeader(summaryTable, "PENDIENTES");
            agregarHeader(summaryTable, "RECHAZADAS");
            summaryTable.addCell(crearCeldaCentrada(String.valueOf(datos.eventos().size() + datos.anuncios().size())));
            summaryTable.addCell(crearCeldaCentrada(String.valueOf(contarEventos(datos.eventos(), EstadoSolicitud.APROBADA) + contarEventos(datos.eventos(), EstadoSolicitud.PUBLICADA) + contarAnuncios(datos.anuncios(), EstadoSolicitud.APROBADA) + contarAnuncios(datos.anuncios(), EstadoSolicitud.PUBLICADA))));
            summaryTable.addCell(crearCeldaCentrada(String.valueOf(contarEventos(datos.eventos(), EstadoSolicitud.PENDIENTE) + contarAnuncios(datos.anuncios(), EstadoSolicitud.PENDIENTE))));
            summaryTable.addCell(crearCeldaCentrada(String.valueOf(contarEventos(datos.eventos(), EstadoSolicitud.RECHAZADA) + contarAnuncios(datos.anuncios(), EstadoSolicitud.RECHAZADA))));
            document.add(summaryTable);
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 3.5f, 2f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f });
            agregarHeader(table, "TIPO");
            agregarHeader(table, "TÍTULO");
            agregarHeader(table, "ESTADO");
            agregarHeader(table, "CREADO EL");
            agregarHeader(table, "ACEPTADO EL");
            agregarHeader(table, "PUBLICADO EL");
            agregarHeader(table, "GESTOR");
            agregarHeader(table, "OFICINA/CAT");
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (SolicitudEvento evento : datos.eventos()) {
                table.addCell(crearCeldaSmall("EVENTO"));
                table.addCell(crearCeldaSmall(nullToEmpty(evento.getNombreEvento())));
                table.addCell(crearCeldaSmall(String.valueOf(evento.getEstado())));
                table.addCell(crearCeldaSmall(formatDateTimeSafe(evento.getFechaCreacion(), dtf)));
                table.addCell(crearCeldaSmall(formatDateTimeSafe(evento.getFechaRevision(), dtf)));
                table.addCell(crearCeldaSmall(formatDateSafe(evento.getFechaEvento(), dtf)));
                table.addCell(crearCeldaSmall(evento.getUsuarioRevisor() != null ? evento.getUsuarioRevisor().getCorreo() : "N/A"));
                table.addCell(crearCeldaSmall(evento.getOficina() != null ? nullToEmpty(evento.getOficina().getNombre()) : "N/A"));
            }
            for (SolicitudAnuncio anuncio : datos.anuncios()) {
                table.addCell(crearCeldaSmall("ANUNCIO"));
                table.addCell(crearCeldaSmall(nullToEmpty(anuncio.getTitulo())));
                table.addCell(crearCeldaSmall(String.valueOf(anuncio.getEstado())));
                table.addCell(crearCeldaSmall(formatDateTimeSafe(anuncio.getFechaCreacion(), dtf)));
                table.addCell(crearCeldaSmall(formatDateTimeSafe(anuncio.getFechaRevision(), dtf)));
                table.addCell(crearCeldaSmall(formatDateSafe(anuncio.getFechaInicioPublicacion(), dtf)));
                table.addCell(crearCeldaSmall(anuncio.getUsuarioRevisor() != null ? anuncio.getUsuarioRevisor().getCorreo() : "N/A"));
                String oficinaText = anuncio.getOficina() != null ? anuncio.getOficina().getNombre() : (anuncio.getCategoria() != null ? anuncio.getCategoria() : "N/A");
                table.addCell(crearCeldaSmall(oficinaText));
            }
            document.add(table);
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Fin del Reporte - Generado automáticamente por CallApp System", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
            footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(footer);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Error crítico al generar PDF de reporte: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al generar PDF");
        }
    }

    private byte[] exportarCsvInterno(DatosReporte datos) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(output), CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader("tipo", "titulo", "estado", "creado_el", "aceptado_el", "publicado_el", "responsable", "revisor", "oficina").build())) {
            for (SolicitudEvento evento : datos.eventos()) {
                csvPrinter.printRecord("EVENTO", evento.getNombreEvento(), evento.getEstado(), evento.getFechaCreacion(), evento.getFechaRevision(), evento.getFechaEvento(), evento.getResponsableEvento(), evento.getUsuarioRevisor() != null ? evento.getUsuarioRevisor().getCorreo() : "N/A", evento.getOficina() != null ? evento.getOficina().getNombre() : "N/A");
            }
            for (SolicitudAnuncio anuncio : datos.anuncios()) {
                csvPrinter.printRecord("ANUNCIO", anuncio.getTitulo(), anuncio.getEstado(), anuncio.getFechaCreacion(), anuncio.getFechaRevision(), anuncio.getFechaInicioPublicacion(), anuncio.getResponsableAnuncio(), anuncio.getUsuarioRevisor() != null ? anuncio.getUsuarioRevisor().getCorreo() : "N/A", anuncio.getOficina() != null ? anuncio.getOficina().getNombre() : (anuncio.getCategoria() != null ? anuncio.getCategoria() : "N/A"));
            }
            csvPrinter.flush();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible generar el CSV");
        }
    }

    private DatosReporte obtenerDatosReporte(LocalDate desde, LocalDate hasta, String alcanceParam, Long idOficina, Long idTipoEvento, Authentication authentication) {
        validarFechas(desde, hasta);
        Usuario usuario = obtenerUsuario(authentication);
        LocalDateTime fechaDesde = desde.atStartOfDay();
        LocalDateTime fechaHasta = hasta.atTime(LocalTime.MAX);
        List<SolicitudEvento> eventos;
        List<SolicitudAnuncio> anuncios;
        String alcanceFinal = alcanceParam != null ? alcanceParam.toUpperCase() : "GLOBAL";

        if (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) {
            if ("ANUNCIOS".equals(alcanceFinal)) {
                eventos = List.of();
            } else if (idOficina != null && idTipoEvento != null) {
                eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(idOficina, fechaDesde, fechaHasta, idTipoEvento);
            } else if (idOficina != null) {
                eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetween(idOficina, fechaDesde, fechaHasta);
            } else if (idTipoEvento != null) {
                eventos = solicitudEventoRepository.findByFechaCreacionBetweenAndTipoEventoCatalogoId(fechaDesde, fechaHasta, idTipoEvento);
            } else {
                eventos = solicitudEventoRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta);
            }

            if ("EVENTOS".equals(alcanceFinal)) {
                anuncios = List.of();
            } else if (idOficina != null) {
                anuncios = solicitudAnuncioRepository.findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(idOficina, fechaDesde, fechaHasta);
            } else {
                anuncios = solicitudAnuncioRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta);
            }
        } else if (usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) {
            Oficina oficina = usuario.getOficina();
            if (oficina == null) {
                eventos = solicitudEventoRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta).stream().filter(e -> e.getUsuarioSolicitante().getId().equals(usuario.getId())).toList();
                anuncios = solicitudAnuncioRepository.findByFechaCreacionBetween(fechaDesde, fechaHasta).stream().filter(a -> a.getUsuarioSolicitante().getId().equals(usuario.getId())).toList();
                alcanceFinal = "PERSONAL";
            } else {
                if ("ANUNCIOS".equals(alcanceFinal)) {
                    eventos = List.of();
                } else if (idTipoEvento != null) {
                    eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetweenAndTipoEventoCatalogoId(oficina.getId(), fechaDesde, fechaHasta, idTipoEvento);
                } else {
                    eventos = solicitudEventoRepository.findByOficinaIdAndFechaCreacionBetween(oficina.getId(), fechaDesde, fechaHasta);
                }
                if ("EVENTOS".equals(alcanceFinal)) {
                    anuncios = List.of();
                } else {
                    anuncios = solicitudAnuncioRepository.findByUsuarioSolicitanteOficinaIdAndFechaCreacionBetween(oficina.getId(), fechaDesde, fechaHasta);
                }
                alcanceFinal = "OFICINA";
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos");
        }
        return new DatosReporte(eventos, anuncios, alcanceFinal);
    }

    private long contarEventos(List<SolicitudEvento> eventos, EstadoSolicitud estado) {
        return eventos.stream().filter(evento -> evento.getEstado() == estado).count();
    }

    private long contarAnuncios(List<SolicitudAnuncio> anuncios, EstadoSolicitud estado) {
        return anuncios.stream().filter(anuncio -> anuncio.getEstado() == estado).count();
    }

    private Usuario obtenerUsuario(Authentication authentication) {
        return usuarioRepository.getByCorreoOptimized(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar fechas");
        if (hasta.isBefore(desde)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha inválida");
    }

    private String normalizarFormato(String formato) {
        if (formato == null || formato.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato requerido");
        String normalizado = formato.trim().toUpperCase();
        if (!normalizado.equals("PDF") && !normalizado.equals("CSV")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no válido");
        return normalizado;
    }

    private ReporteGenerado buscarReporte(Long reporteId, Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        ReporteGenerado reporte = reporteGeneradoRepository.findById(Objects.requireNonNull(reporteId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontrado"));
        if (usuario.getRol().esAdministradorGlobal() || usuario.getRol().esComunicaciones()) return reporte;
        boolean esMismoUsuario = reporte.getUsuarioGenerador().getId().equals(usuario.getId());
        boolean esMismaOficina = usuario.getOficina() != null && reporte.getUsuarioGenerador().getOficina() != null && usuario.getOficina().getId().equals(reporte.getUsuarioGenerador().getOficina().getId());
        if ((usuario.getRol().esOficina() || usuario.getRol().name().equals("USUARIO_AUTENTICADO_APP")) && (esMismoUsuario || esMismaOficina)) return reporte;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
    }

    @SuppressWarnings("null")
    public ReporteGeneradoResponse actualizarReporte(Long id, ActualizarReporteRequest request, Authentication authentication) {
        ReporteGenerado reporte = buscarReporte(id, authentication);
        if (request.getNombre() != null && !request.getNombre().isBlank()) reporte.setNombre(request.getNombre());
        if (request.getDescripcion() != null) reporte.setDescripcion(request.getDescripcion());
        return toReporteGeneradoResponse(reporteGeneradoRepository.save(reporte));
    }

    private ReporteGeneradoResponse toReporteGeneradoResponse(ReporteGenerado reporte) {
        return ReporteGeneradoResponse.builder()
                .id(reporte.getId())
                .nombre(reporte.getNombre())
                .descripcion(reporte.getDescripcion())
                .formato(reporte.getFormato())
                .desde(reporte.getFechaDesde())
                .hasta(reporte.getFechaHasta())
                .alcance(reporte.getAlcance())
                .fechaCreacion(reporte.getFechaCreacion())
                .usuarioGeneradorId(reporte.getUsuarioGenerador() != null ? reporte.getUsuarioGenerador().getId() : null)
                .usuarioGeneradorCorreo(reporte.getUsuarioGenerador() != null ? reporte.getUsuarioGenerador().getCorreo() : "SISTEMA")
                .usuarioGeneradorOficina(reporte.getUsuarioGenerador() != null && reporte.getUsuarioGenerador().getOficina() != null ? reporte.getUsuarioGenerador().getOficina().getNombre() : "N/A")
                .idOficina(reporte.getIdOficina())
                .idTipoEvento(reporte.getIdTipoEvento())
                .build();
    }

    private void agregarHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE)));
        cell.setBackgroundColor(java.awt.Color.decode("#CE1126"));
        cell.setPadding(6);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell crearCeldaSmall(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setPadding(4);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell crearCeldaCentrada(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.decode("#ce1126"))));
        cell.setPadding(10);
        cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        return cell;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatDateSafe(LocalDate date, java.time.format.DateTimeFormatter dtf) {
        return date != null ? date.format(dtf) : "-";
    }

    private String formatDateTimeSafe(LocalDateTime dateTime, java.time.format.DateTimeFormatter dtf) {
        return dateTime != null ? dateTime.format(dtf) : "-";
    }

    private record DatosReporte(List<SolicitudEvento> eventos, List<SolicitudAnuncio> anuncios, String alcance) {}

    private SolicitudResumenDTO mapToResumen(SolicitudEvento e) {
        return SolicitudResumenDTO.builder()
                .id(e.getId())
                .tipo("EVENTO")
                .titulo(e.getNombreEvento() != null ? e.getNombreEvento() : "Sin Título")
                .oficina(e.getOficina() != null ? e.getOficina().getNombre() : "N/A")
                .fechaRegistro(e.getFechaCreacion() != null ? e.getFechaCreacion().toString().split("T")[0] : "-")
                .estado(e.getEstado() != null ? e.getEstado().toString() : "PENDIENTE")
                .build();
    }

    private SolicitudResumenDTO mapToResumen(SolicitudAnuncio a) {
        return SolicitudResumenDTO.builder()
                .id(a.getId())
                .tipo("ANUNCIO")
                .titulo(a.getTitulo() != null ? a.getTitulo() : "Sin Título")
                .oficina(a.getOficina() != null ? a.getOficina().getNombre() : "N/A")
                .fechaRegistro(a.getFechaCreacion() != null ? a.getFechaCreacion().toString().split("T")[0] : "-")
                .estado(a.getEstado() != null ? a.getEstado().toString() : "PENDIENTE")
                .build();
    }
}

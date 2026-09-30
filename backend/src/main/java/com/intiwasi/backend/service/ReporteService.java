package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.dto.inventario.StockBajoResponse;
import com.intiwasi.backend.dto.reporte.InventarioActualResponse;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private static final int MAX_PAGINA = 200;
    private static final int MAX_EXPORTACION = 1000;

    private final ProductoRepository productoRepository;
    private final StockBajoRepository stockBajoRepository;
    private final MovimientoConsultaService movimientoConsultaService;

    @Transactional(readOnly = true)
    public Page<InventarioActualResponse> inventario(Integer idProducto, Pageable pageable) {
        return productoRepository.buscarInventario(idProducto, limitar(pageable)).map(this::convertirProducto);
    }

    @Transactional(readOnly = true)
    public Page<StockBajoResponse> reposicion(Integer idProducto, Pageable pageable) {
        return stockBajoRepository.buscar(idProducto, limitar(pageable)).map(this::convertirStockBajo);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoConsultaResponse> movimientos(LocalDate inicio, LocalDate fin, Integer idProducto,
                                                         TipoDocumento tipo, Integer idUsuario, Pageable pageable) {
        return movimientoConsultaService.consultar(inicio, fin, idProducto, tipo, idUsuario, limitar(pageable));
    }

    public byte[] exportar(String reporte, String formato, LocalDate inicio, LocalDate fin,
                           Integer idProducto, TipoDocumento tipo, Integer idUsuario) {
        Tabla tabla = consultarTabla(reporte, inicio, fin, idProducto, tipo, idUsuario);
        return switch (formato.toLowerCase()) {
            case "pdf" -> generarPdf(tabla);
            case "excel", "xls" -> generarExcelXml(tabla);
            default -> throw new IllegalArgumentException("Formato de exportación inválido; use pdf o excel");
        };
    }

    private Tabla consultarTabla(String reporte, LocalDate inicio, LocalDate fin, Integer idProducto,
                                 TipoDocumento tipo, Integer idUsuario) {
        Pageable pagina = PageRequest.of(0, MAX_EXPORTACION);
        return switch (reporte.toLowerCase()) {
            case "inventario" -> new Tabla(List.of("SKU", "Producto", "Categoría", "Proveedor", "Stock", "Mínimo"),
                    inventario(idProducto, pagina).getContent().stream()
                            .map(i -> List.of(i.getSku(), i.getNomProducto(), i.getCategoria(), nulo(i.getProveedor()),
                                    i.getStockActual().toString(), i.getStockMinimo().toString())).toList());
            case "movimientos" -> new Tabla(List.of("Fecha", "Tipo", "SKU", "Producto", "Cantidad", "Motivo", "Usuario"),
                    movimientos(inicio, fin, idProducto, tipo, idUsuario, pagina).getContent().stream()
                            .map(m -> List.of(m.getFechaEmision().toString(), m.getTipoDocumento().getValor(), m.getSku(),
                                    m.getNomProducto(), m.getCantidadConSigno().toString(), m.getMotivo(), m.getUsuarioResponsable())).toList());
            case "reposicion" -> new Tabla(List.of("SKU", "Producto", "Categoría", "Proveedor", "Stock", "Mínimo", "Reponer"),
                    reposicion(idProducto, pagina).getContent().stream()
                            .map(s -> List.of(s.getSku(), s.getNomProducto(), s.getNomCategoria(), nulo(s.getNomProveedor()),
                                    s.getStockActual().toString(), s.getStockMinimo().toString(), s.getUnidadesPorReponer().toString())).toList());
            default -> throw new IllegalArgumentException("Reporte inválido; use inventario, movimientos o reposicion");
        };
    }

    private Pageable limitar(Pageable pageable) {
        int pagina = Math.max(0, pageable.getPageNumber());
        int tamano = Math.max(1, Math.min(MAX_PAGINA, pageable.getPageSize()));
        return PageRequest.of(pagina, tamano, pageable.getSort());
    }

    private InventarioActualResponse convertirProducto(Producto p) {
        return InventarioActualResponse.builder().idProducto(p.getIdProducto()).sku(p.getSku())
                .nomProducto(p.getNomProducto()).categoria(p.getCategoria().getNomCategoria())
                .proveedor(p.getProveedor() == null ? null : p.getProveedor().getNomProveedor())
                .precio(p.getPrecio()).stockActual(p.getStockActual()).stockMinimo(p.getStockMinimo()).build();
    }

    private StockBajoResponse convertirStockBajo(StockBajo s) {
        return StockBajoResponse.builder().idProducto(s.getIdProducto()).sku(s.getSku())
                .nomProducto(s.getNomProducto()).nomCategoria(s.getNomCategoria()).nomProveedor(s.getNomProveedor())
                .stockActual(s.getStockActual()).stockMinimo(s.getStockMinimo())
                .unidadesPorReponer(s.getUnidadesPorReponer()).build();
    }

    private byte[] generarExcelXml(Tabla tabla) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<?mso-application progid=\"Excel.Sheet\"?>")
                .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"><Worksheet ss:Name=\"Reporte\"><Table>");
        agregarFilaXml(xml, tabla.encabezados());
        tabla.filas().forEach(fila -> agregarFilaXml(xml, fila));
        xml.append("</Table></Worksheet></Workbook>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void agregarFilaXml(StringBuilder xml, List<String> fila) {
        xml.append("<Row>");
        fila.forEach(valor -> xml.append("<Cell><Data ss:Type=\"String\">")
                .append(escaparXml(valor)).append("</Data></Cell>"));
        xml.append("</Row>");
    }

    private byte[] generarPdf(Tabla tabla) {
        final int filasPorPagina = 45;
        int cantidadPaginas = Math.max(1, (tabla.filas().size() + filasPorPagina - 1) / filasPorPagina);
        int objetoFuente = 3 + cantidadPaginas * 2;
        StringBuilder hijos = new StringBuilder();
        for (int pagina = 0; pagina < cantidadPaginas; pagina++) {
            hijos.append(3 + pagina * 2).append(" 0 R ");
        }

        List<String> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        objetos.add("<< /Type /Pages /Kids [" + hijos + "] /Count " + cantidadPaginas + " >>");
        for (int pagina = 0; pagina < cantidadPaginas; pagina++) {
            int objetoContenido = 4 + pagina * 2;
            String contenido = contenidoPagina(tabla, pagina * filasPorPagina,
                    Math.min(tabla.filas().size(), (pagina + 1) * filasPorPagina));
            objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                    + objetoFuente + " 0 R >> >> /Contents " + objetoContenido + " 0 R >>");
            objetos.add("<< /Length " + contenido.getBytes(StandardCharsets.ISO_8859_1).length
                    + " >>\nstream\n" + contenido + "\nendstream");
        }
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        escribir(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(out.size());
            escribir(out, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
        }
        int xref = out.size();
        escribir(out, "xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        offsets.forEach(offset -> escribir(out, String.format("%010d 00000 n \n", offset)));
        escribir(out, "trailer << /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    private String contenidoPagina(Tabla tabla, int desde, int hasta) {
        StringBuilder contenido = new StringBuilder("BT /F1 8 Tf 30 810 Td 11 TL ");
        List<String> lineas = new ArrayList<>();
        lineas.add(String.join(" | ", tabla.encabezados()));
        tabla.filas().subList(desde, hasta).forEach(fila -> lineas.add(String.join(" | ", fila)));
        for (String linea : lineas) {
            String corta = linea.length() > 125 ? linea.substring(0, 125) : linea;
            contenido.append('(').append(escaparPdf(corta)).append(") Tj T* ");
        }
        return contenido.append("ET").toString();
    }

    private void escribir(ByteArrayOutputStream out, String valor) {
        out.writeBytes(valor.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String escaparXml(String valor) { return nulo(valor).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String escaparPdf(String valor) { return nulo(valor).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); }
    private String nulo(String valor) { return valor == null ? "" : valor; }

    private record Tabla(List<String> encabezados, List<List<String>> filas) {}
}

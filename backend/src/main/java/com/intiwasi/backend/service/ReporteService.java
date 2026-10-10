package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.dto.inventario.StockBajoResponse;
import com.intiwasi.backend.dto.reporte.InventarioActualResponse;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.StockBajo;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.StockBajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private static final int MAX_PAGINA = 200;
    private static final int MAX_EXPORTACION = 10_000;

    private final ProductoRepository productoRepository;
    private final StockBajoRepository stockBajoRepository;
    private final MovimientoConsultaService movimientoConsultaService;
    private final ReportePdfWriter pdfWriter;

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

    @Transactional(readOnly = true)
    public Optional<byte[]> exportar(String reporte, String formato, LocalDate inicio, LocalDate fin,
                            Integer idProducto, TipoDocumento tipo, Integer idUsuario) {
        if (!List.of("pdf", "csv").contains(formato.toLowerCase())) {
            throw new IllegalArgumentException("Formato de exportación inválido; use pdf o csv");
        }
        Tabla tabla = consultarTabla(reporte, inicio, fin, idProducto, tipo, idUsuario);
        if (tabla.filas().isEmpty()) return Optional.empty();
        return Optional.of(switch (formato.toLowerCase()) {
            case "pdf" -> pdfWriter.escribir(reporte, tabla.encabezados(), tabla.filas());
            case "csv" -> generarCsv(tabla);
            default -> throw new IllegalStateException("Formato validado no reconocido");
        });
    }

    private Tabla consultarTabla(String reporte, LocalDate inicio, LocalDate fin, Integer idProducto,
                                 TipoDocumento tipo, Integer idUsuario) {
        return switch (reporte.toLowerCase()) {
            case "inventario" -> new Tabla(List.of("SKU", "Producto", "Categoría", "Proveedor", "Precio referencial", "Stock", "Mínimo", "Estado"),
                    reunir(pagina -> inventario(idProducto, pagina)).stream()
                            .map(i -> List.of(i.getSku(), i.getNomProducto(), i.getCategoria(), nulo(i.getProveedor()),
                                    i.getPrecio().toPlainString(), i.getStockActual().toString(), i.getStockMinimo().toString(),
                                    i.getStockActual() <= i.getStockMinimo() ? "Stock bajo" : "Normal")).toList());
            case "movimientos" -> new Tabla(List.of("Fecha", "Tipo", "SKU", "Producto", "Cantidad", "Motivo", "Usuario"),
                    reunir(pagina -> movimientos(inicio, fin, idProducto, tipo, idUsuario, pagina)).stream()
                            .map(m -> List.of(m.getFechaEmision().toString(), m.getTipoDocumento().getValor(), m.getSku(),
                                    m.getNomProducto(), m.getCantidadConSigno().toString(), nulo(m.getMotivo()), m.getUsuarioResponsable())).toList());
            case "reposicion" -> new Tabla(List.of("SKU", "Producto", "Categoría", "Proveedor", "Stock", "Mínimo", "Reponer"),
                    reunir(pagina -> reposicion(idProducto, pagina)).stream()
                            .map(s -> List.of(s.getSku(), s.getNomProducto(), s.getNomCategoria(), nulo(s.getNomProveedor()),
                                    s.getStockActual().toString(), s.getStockMinimo().toString(), s.getUnidadesPorReponer().toString())).toList());
            default -> throw new IllegalArgumentException("Reporte inválido; use inventario, movimientos o reposicion");
        };
    }

    private <T> List<T> reunir(Function<Pageable, Page<T>> consulta) {
        Page<T> primera = consulta.apply(PageRequest.of(0, MAX_PAGINA));
        if (primera.getTotalElements() > MAX_EXPORTACION) {
            throw new ReglaNegocioException(
                    "El reporte supera 10 000 registros. Aplica filtros para reducir los resultados");
        }
        List<T> filas = new ArrayList<>(primera.getContent());
        for (int pagina = 1; pagina < primera.getTotalPages(); pagina++) {
            filas.addAll(consulta.apply(PageRequest.of(pagina, MAX_PAGINA)).getContent());
        }
        return filas;
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

    private byte[] generarCsv(Tabla tabla) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        agregarFilaCsv(csv, tabla.encabezados());
        tabla.filas().forEach(fila -> agregarFilaCsv(csv, fila));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void agregarFilaCsv(StringBuilder csv, List<String> fila) {
        for (int i = 0; i < fila.size(); i++) {
            if (i > 0) csv.append(',');
            String valor = nulo(fila.get(i));
            String inicio = valor.stripLeading();
            if (!inicio.isEmpty() && (inicio.charAt(0) == '=' || inicio.charAt(0) == '+'
                    || inicio.charAt(0) == '@' || inicio.charAt(0) == '-'
                    && (inicio.length() == 1 || !Character.isDigit(inicio.charAt(1))))) {
                valor = "'" + valor;
            }
            csv.append('"').append(valor.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String nulo(String valor) { return valor == null ? "" : valor; }

    private record Tabla(List<String> encabezados, List<List<String>> filas) {}
}

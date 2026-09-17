package com.intiwasi.backend.dto.Categoria;

import lombok.Data;

@Data
public class CategoriaResponse {

    private Integer idCategoria;
    private String nomCategoria;
    private Byte estado;
}

package com.intiwasi.backend.entity.converter;

import com.intiwasi.backend.entity.enums.TipoDocumento;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoDocumentoConverter implements AttributeConverter<TipoDocumento, String> {
    @Override
    public String convertToDatabaseColumn(TipoDocumento attribute) {
        return attribute == null ? null : attribute.getValor();
    }

    @Override
    public TipoDocumento convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoDocumento.desdeValor(dbData);
    }
}

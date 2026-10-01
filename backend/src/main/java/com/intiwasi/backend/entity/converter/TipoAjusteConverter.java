package com.intiwasi.backend.entity.converter;

import com.intiwasi.backend.entity.enums.TipoAjuste;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoAjusteConverter implements AttributeConverter<TipoAjuste, String> {
    @Override
    public String convertToDatabaseColumn(TipoAjuste attribute) {
        return attribute == null ? null : attribute.getValor();
    }

    @Override
    public TipoAjuste convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoAjuste.desdeValor(dbData);
    }
}

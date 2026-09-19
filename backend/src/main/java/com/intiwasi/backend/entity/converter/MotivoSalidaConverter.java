package com.intiwasi.backend.entity.converter;

import com.intiwasi.backend.entity.enums.MotivoSalida;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MotivoSalidaConverter implements AttributeConverter<MotivoSalida, String> {
    @Override
    public String convertToDatabaseColumn(MotivoSalida attribute) {
        return attribute == null ? null : attribute.getValor();
    }

    @Override
    public MotivoSalida convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MotivoSalida.desdeValor(dbData);
    }
}

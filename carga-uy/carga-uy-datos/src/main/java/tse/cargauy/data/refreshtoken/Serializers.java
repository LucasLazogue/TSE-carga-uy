package tse.cargauy.data.refreshtoken;

import tse.cargauy.dtos.RefreshTokenDto;
import tse.cargauy.entities.RefreshToken;

public class Serializers {
    public static RefreshToken toEntity(RefreshTokenDto dto) {
        return new RefreshToken(dto.getHash(), dto.getIdUsuario(), dto.getDispositivo(), dto.getEstado(),
                dto.getEmitido(), dto.getUltimoUso(), dto.getExpira());
    }

    public static RefreshTokenDto toDto(RefreshToken entity) {
        return new RefreshTokenDto(entity.getHash(), entity.getIdUsuario(), entity.getDispositivo(), entity.getEstado(),
                entity.getEmitido(), entity.getUltimoUso(), entity.getExpira());
    }
}

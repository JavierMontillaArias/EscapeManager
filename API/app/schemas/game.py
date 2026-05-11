from pydantic import BaseModel, ConfigDict, Field
from datetime import datetime


class GameCloseRequest(BaseModel):
    escaparon: bool
    observaciones: str | None = Field(default=None, max_length=2000)


class GameMasterInfo(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    nombre: str


class GameResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    reserva_id: int
    gamemaster_id: int
    pistas_usadas: int
    hora_inicio_real: datetime
    hora_fin_real: datetime | None
    escaparon: bool | None
    observaciones: str | None
    gamemaster: GameMasterInfo


class GameHintResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    pistas_usadas: int
    message: str = "Pista registrada correctamente"
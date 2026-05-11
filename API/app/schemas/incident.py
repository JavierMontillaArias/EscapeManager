from pydantic import BaseModel, ConfigDict, Field
from datetime import datetime


class IncidentCreate(BaseModel):
    sala_id: int = Field(ge=1)
    descripcion: str = Field(min_length=10, max_length=1000)


class IncidentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    sala_id: int
    usuario_id: int
    descripcion: str
    fecha: datetime
    resuelta: bool
    fecha_resolucion: datetime | None


class IncidentResolveResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    resuelta: bool
    fecha_resolucion: datetime
    message: str = "Incidencia marcada como resuelta"
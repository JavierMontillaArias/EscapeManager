from pydantic import BaseModel, ConfigDict, Field
from app.models.room import Dificultad


class RoomBase(BaseModel):
    nombre: str = Field(min_length=2, max_length=100)
    tematica: str = Field(min_length=2, max_length=100)
    capacidad_max: int = Field(ge=1, le=100)
    dificultad: Dificultad


class RoomCreate(RoomBase):
    pass


class RoomUpdate(BaseModel):
    nombre: str | None = Field(default=None, min_length=2, max_length=100)
    tematica: str | None = Field(default=None, min_length=2, max_length=100)
    capacidad_max: int | None = Field(default=None, ge=1, le=100)
    dificultad: Dificultad | None = None
    activa: bool | None = None


class RoomResponse(RoomBase):
    model_config = ConfigDict(from_attributes=True)

    id: int
    activa: bool
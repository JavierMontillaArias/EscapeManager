from pydantic import BaseModel, EmailStr, ConfigDict, Field, field_validator
from datetime import date, time, datetime
from app.models.booking import EstadoReserva
from app.schemas.room import RoomResponse


class BookingBase(BaseModel):
    nombre_grupo: str = Field(min_length=2, max_length=150)
    num_personas: int = Field(ge=1, le=100)
    email_cliente: EmailStr
    fecha: date
    hora_inicio: time
    hora_fin: time
    sala_id: int = Field(ge=1)

    @field_validator("hora_fin")
    @classmethod
    def hora_fin_debe_ser_posterior(cls, hora_fin: time, info) -> time:
        hora_inicio = info.data.get("hora_inicio")
        if hora_inicio and hora_fin <= hora_inicio:
            raise ValueError("hora_fin debe ser posterior a hora_inicio")
        return hora_fin


class BookingCreate(BookingBase):
    pass


class BookingUpdate(BaseModel):
    nombre_grupo: str | None = Field(default=None, min_length=2, max_length=150)
    num_personas: int | None = Field(default=None, ge=1, le=100)
    email_cliente: EmailStr | None = None
    fecha: date | None = None
    hora_inicio: time | None = None
    hora_fin: time | None = None
    sala_id: int | None = Field(default=None, ge=1)
    estado: EstadoReserva | None = None


class BookingResponse(BookingBase):
    model_config = ConfigDict(from_attributes=True)

    id: int
    estado: EstadoReserva
    qr_token: str
    qr_usado: bool
    sala: RoomResponse
    created_at: datetime


class QRValidateRequest(BaseModel):
    token: str


class QRValidateResponse(BaseModel):
    message: str
    partida_id: int
    reserva_id: int
    sala: str
    hora_inicio_real: str
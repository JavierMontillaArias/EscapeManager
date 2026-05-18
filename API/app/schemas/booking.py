from pydantic import BaseModel, EmailStr, ConfigDict, Field, field_validator, model_validator
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

    # SEC-01: primera línea de defensa para validar coherencia horaria en PUT.
    # El servicio también lo valida, pero el schema rechaza el request antes.
    @model_validator(mode='after')
    def check_hora_fin(self) -> 'BookingUpdate':
        if self.hora_inicio and self.hora_fin and self.hora_fin <= self.hora_inicio:
            raise ValueError("hora_fin debe ser posterior a hora_inicio")
        return self


class BookingResponse(BookingBase):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    estado: EstadoReserva
    qr_token: str
    qr_usado: bool
    # PR-02: validation_alias mapea el campo ORM qr_enviado al nombre de API qr_email_enviado.
    qr_email_enviado: bool = Field(default=False, validation_alias="qr_enviado")
    sala: RoomResponse
    created_at: datetime


class BookingStatusUpdate(BaseModel):
    estado: EstadoReserva


class MessageResponse(BaseModel):
    message: str


class QRValidateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    token: str = Field(alias="qr_token")


class QRValidateResponse(BaseModel):
    message: str
    game_id: int
    reserva_id: int
    nombre_grupo: str
    sala: str
    hora_inicio_real: str

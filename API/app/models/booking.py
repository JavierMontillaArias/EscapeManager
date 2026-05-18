import enum
import uuid
from datetime import date, time, datetime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Integer, Boolean, Date, Time, DateTime, ForeignKey, Enum as SAEnum, func
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.room import Room
    from app.models.game import Game


class EstadoReserva(str, enum.Enum):
    pendiente = "pendiente"
    confirmada = "confirmada"
    en_curso = "en_curso"
    completada = "completada"
    cancelada = "cancelada"


class Booking(Base):
    __tablename__ = "bookings"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    nombre_grupo: Mapped[str] = mapped_column(String(150), nullable=False)
    num_personas: Mapped[int] = mapped_column(Integer, nullable=False)
    email_cliente: Mapped[str] = mapped_column(String(255), nullable=False)
    fecha: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    hora_inicio: Mapped[time] = mapped_column(Time, nullable=False)
    hora_fin: Mapped[time] = mapped_column(Time, nullable=False)
    estado: Mapped[EstadoReserva] = mapped_column(
        SAEnum(EstadoReserva, name="estado_reserva_enum"),
        default=EstadoReserva.pendiente,
        nullable=False,
        index=True,  # PERF-03: acelera queries de estadísticas que filtran por estado
    )

    # MySQL no tiene tipo UUID nativo → String(36) en formato estándar
    qr_token: Mapped[str] = mapped_column(
        String(36),
        default=lambda: str(uuid.uuid4()),
        unique=True,
        nullable=False,
        index=True,
    )
    qr_usado: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    # PR-02: Indica si el email de confirmación con el QR fue enviado con éxito.
    # Permite al Manager detectar envíos fallidos y usar el endpoint /resend-qr.
    qr_enviado: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        server_default=func.now(),
        nullable=False,
    )

    sala_id: Mapped[int] = mapped_column(ForeignKey("rooms.id"), nullable=False)

    sala: Mapped["Room"] = relationship("Room", back_populates="reservas")
    partida: Mapped["Game | None"] = relationship(
        "Game", back_populates="reserva", uselist=False
    )

    def __repr__(self) -> str:
        return f"<Booking id={self.id} grupo={self.nombre_grupo!r} estado={self.estado}>"

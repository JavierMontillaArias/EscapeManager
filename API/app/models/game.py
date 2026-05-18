from datetime import datetime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import Integer, Boolean, Text, DateTime, ForeignKey
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.booking import Booking
    from app.models.user import User


class Game(Base):
    __tablename__ = "games"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    pistas_usadas: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    hora_inicio_real: Mapped[datetime] = mapped_column(DateTime, nullable=False, index=True)
    # Nullable: la partida puede estar en curso sin hora de fin
    hora_fin_real: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, index=True)
    # Nullable: se desconoce hasta que la partida se cierra
    escaparon: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    observaciones: Mapped[str | None] = mapped_column(Text, nullable=True)

    # unique=True garantiza que una reserva solo puede tener una partida
    reserva_id: Mapped[int] = mapped_column(
        ForeignKey("bookings.id"), unique=True, nullable=False
    )
    gamemaster_id: Mapped[int] = mapped_column(
        ForeignKey("users.id"), nullable=False
    )

    reserva: Mapped["Booking"] = relationship("Booking", back_populates="partida")
    gamemaster: Mapped["User"] = relationship("User", back_populates="partidas")

    def __repr__(self) -> str:
        return f"<Game id={self.id} reserva={self.reserva_id} escaparon={self.escaparon}>"
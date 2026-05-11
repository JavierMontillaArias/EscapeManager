import enum
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Integer, Boolean, Enum as SAEnum
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.booking import Booking
    from app.models.incident import Incident


class Dificultad(str, enum.Enum):
    """
    Heredar de str permite serializar el enum directamente a JSON
    sin conversión adicional en los schemas Pydantic.
    """
    baja = "baja"
    media = "media"
    alta = "alta"
    extrema = "extrema"


class Room(Base):
    __tablename__ = "rooms"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    nombre: Mapped[str] = mapped_column(String(100), nullable=False)
    tematica: Mapped[str] = mapped_column(String(100), nullable=False)
    capacidad_max: Mapped[int] = mapped_column(Integer, nullable=False)
    dificultad: Mapped[Dificultad] = mapped_column(
        SAEnum(Dificultad, name="dificultad_enum"),
        nullable=False,
    )
    activa: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    reservas: Mapped[list["Booking"]] = relationship("Booking", back_populates="sala")
    incidencias: Mapped[list["Incident"]] = relationship("Incident", back_populates="sala")

    def __repr__(self) -> str:
        return f"<Room id={self.id} nombre={self.nombre!r}>"
from datetime import datetime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Boolean, DateTime, ForeignKey, func
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.room import Room
    from app.models.user import User


class Incident(Base):
    __tablename__ = "incidents"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    descripcion: Mapped[str] = mapped_column(String(1000), nullable=False)
    fecha: Mapped[datetime] = mapped_column(
        DateTime,
        server_default=func.now(),
        nullable=False,
    )
    resuelta: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    fecha_resolucion: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    sala_id: Mapped[int] = mapped_column(ForeignKey("rooms.id"), nullable=False)
    usuario_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)

    sala: Mapped["Room"] = relationship("Room", back_populates="incidencias")
    usuario: Mapped["User"] = relationship("User", back_populates="incidencias")

    def __repr__(self) -> str:
        return f"<Incident id={self.id} sala={self.sala_id} resuelta={self.resuelta}>"
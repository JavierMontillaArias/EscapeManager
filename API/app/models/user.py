from datetime import datetime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String, Boolean, ForeignKey, DateTime, func
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.role import Role
    from app.models.game import Game
    from app.models.incident import Incident


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    nombre: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    activo: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    # MySQL: DateTime sin timezone. Guardamos siempre en UTC desde Python.
    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        server_default=func.now(),
        nullable=False,
    )

    rol_id: Mapped[int] = mapped_column(ForeignKey("roles.id"), nullable=False)

    rol: Mapped["Role"] = relationship("Role", back_populates="usuarios")
    partidas: Mapped[list["Game"]] = relationship("Game", back_populates="gamemaster")
    incidencias: Mapped[list["Incident"]] = relationship("Incident", back_populates="usuario")

    @property
    def rol_nombre(self) -> str:
        return self.rol.nombre if self.rol else ""

    def __repr__(self) -> str:
        return f"<User id={self.id} email={self.email!r}>"

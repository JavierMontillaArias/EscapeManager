from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import String
from typing import TYPE_CHECKING
from app.database import Base

if TYPE_CHECKING:
    from app.models.user import User


class Role(Base):
    __tablename__ = "roles"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    nombre: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    descripcion: Mapped[str | None] = mapped_column(String(255), nullable=True)

    usuarios: Mapped[list["User"]] = relationship("User", back_populates="rol")

    def __repr__(self) -> str:
        return f"<Role id={self.id} nombre={self.nombre!r}>"
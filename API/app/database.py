from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase, Session
from typing import Generator
from app.config import settings


# pool_pre_ping=True: verifica que la conexión sigue viva antes de usarla.
# Evita errores de "connection closed" tras periodos de inactividad.
engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10,
    echo=False,
)

SessionLocal = sessionmaker(
    bind=engine,
    autocommit=False,
    autoflush=False,
)


class Base(DeclarativeBase):
    pass


def get_db() -> Generator[Session, None, None]:
    """
    Genera una sesión de base de datos por request.
    El try/finally garantiza que la sesión se cierra siempre.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
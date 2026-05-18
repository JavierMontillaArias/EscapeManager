import warnings
from functools import lru_cache
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Base de datos ──────────────────────────────────────────
    DATABASE_URL: str

    # ── JWT ────────────────────────────────────────────────────
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # ── SMTP ───────────────────────────────────────────────────
    SMTP_HOST: str = "smtp.gmail.com"
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM: str = ""

    # ── General ────────────────────────────────────────────────
    ENVIRONMENT: str = "development"
    ALLOWED_ORIGINS: list[str] = ["http://localhost:8000"]

    # B-05: Zona horaria del negocio para validación QR.
    # Las horas de reserva se almacenan en hora local; el servidor debe
    # usar la misma zona horaria para que la ventana ±15 min sea correcta.
    TIMEZONE: str = "UTC"

    @field_validator("SECRET_KEY")
    @classmethod
    def validate_secret_key(cls, v: str) -> str:
        if len(v) < 32:
            raise ValueError("SECRET_KEY debe tener al menos 32 caracteres para garantizar seguridad JWT")
        return v

    @field_validator("DATABASE_URL")
    @classmethod
    def validate_database_url(cls, v: str) -> str:
        """
        Normaliza la URL de MySQL para garantizar compatibilidad.
        Railway u otros proveedores pueden entregar mysql:// sin el driver,
        lo convertimos a mysql+pymysql:// que es lo que SQLAlchemy necesita.
        """
        if v.startswith("mysql://"):
            v = v.replace("mysql://", "mysql+pymysql://", 1)
        return v

    # S-05: Advertir si ALLOWED_ORIGINS contiene wildcard o solo localhost en producción.
    @field_validator("ALLOWED_ORIGINS")
    @classmethod
    def validate_origins(cls, v: list[str]) -> list[str]:
        if "*" in v:
            warnings.warn(
                "CORS wildcard '*' no es seguro para producción. "
                "Configura ALLOWED_ORIGINS con dominios específicos.",
                stacklevel=2,
            )
        return v

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
    )


@lru_cache
def get_settings() -> Settings:
    """
    Retorna una instancia singleton de Settings.
    @lru_cache garantiza que .env se lee una sola vez durante
    todo el ciclo de vida de la aplicación.
    """
    return Settings()


settings = get_settings()
